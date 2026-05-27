package com.tayota.operationservice.service.chat;

import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.util.SecurityContextUtil;
import com.tayota.operationservice.dto.response.chat.ChatMessageResponseDTO;
import com.tayota.operationservice.dto.response.chat.ChatSessionResponseDTO;
import com.tayota.operationservice.entity.chat.ChatMessage;
import com.tayota.operationservice.entity.chat.ChatSession;
import com.tayota.operationservice.enums.chat.ChatSenderType;
import com.tayota.operationservice.enums.chat.ChatSessionStatus;
import com.tayota.operationservice.mapper.chat.ChatMapper;
import com.tayota.operationservice.repository.chat.ChatMessageRepository;
import com.tayota.operationservice.repository.chat.ChatSessionRepository;
import com.tayota.operationservice.repository.user.UserProfileRepository;
import com.tayota.operationservice.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {
    private static final String CHAT_SESSION_TOPIC_PREFIX = "/topic/chat.sessions.";
    private static final String ASSISTANT_CHAT_SESSIONS_TOPIC = "/topic/assistant.chat.sessions";
    private static final List<ChatSessionStatus> OPEN_SESSION_STATUSES = List.of(
            ChatSessionStatus.WAITING,
            ChatSessionStatus.CHATTING
    );
    private static final List<ChatSessionStatus> MERGE_SESSION_STATUSES = List.of(
            ChatSessionStatus.WAITING,
            ChatSessionStatus.CHATTING,
            ChatSessionStatus.RESOLVED
    );
    private static final String GUEST_DISPLAY_NAME = "Khách vãng lai";

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserProfileRepository userProfileRepository;
    private final CookieUtil cookieUtil;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMapper chatMapper;

    // Gửi tin nhắn khách hàng trong phiên chat hiện tại
    @Transactional
    public ChatMessageResponseDTO customerSendMessage(
            String content,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // Lấy hoặc tạo phiên chat hiện tại từ cookie chat_session
        ChatSession chatSession = getOrCreateCurrentSession(request, response);

        // Mở lại phiên chat nếu phiên đã xử lý hoặc đã đóng
        chatSession = reopenSessionIfFinished(chatSession);

        // Tạo tin nhắn khách hàng và lưu vào database
        ChatMessageResponseDTO chatMessage = createMessageResponse(
                chatSession,
                getCurrentUserIdOrNull(),
                ChatSenderType.CUSTOMER,
                content
        );

        // Gửi tin nhắn mới đến topic của phiên chat
        publishChatMessage(chatSession.getId(), chatMessage);

        // Gửi trạng thái phiên chat mới nhất đến danh sách assistant
        publishChatSessionUpdate(chatSession);

        // Trả về tin nhắn vừa tạo cho client gọi API
        return chatMessage;
    }

    // Gửi tin nhắn khách hàng vào một phiên chat cụ thể
    @Transactional
    public ChatMessageResponseDTO customerSendMessageToSession(UUID chatSessionId, String content) {
        // Tìm phiên chat theo ID nhận từ header chat_session
        ChatSession chatSession = findChatSessionWithoutLock(chatSessionId);

        // Mở lại phiên chat nếu phiên đã xử lý hoặc đã đóng
        chatSession = reopenSessionIfFinished(chatSession);

        // Tạo tin nhắn khách hàng và lưu vào database
        ChatMessageResponseDTO chatMessage = createMessageResponse(
                chatSession,
                getCurrentUserIdOrNull(),
                ChatSenderType.CUSTOMER,
                content
        );

        // Gửi tin nhắn mới đến topic của phiên chat
        publishChatMessage(chatSession.getId(), chatMessage);

        // Gửi trạng thái phiên chat mới nhất đến danh sách assistant
        publishChatSessionUpdate(chatSession);

        // Trả về tin nhắn vừa tạo cho client WebSocket
        return chatMessage;
    }

    // Gửi tin nhắn assistant trong một phiên chat
    @Transactional
    public ChatMessageResponseDTO assistantSendMessage(UUID chatSessionId, String content) {
        // Yêu cầu assistant phải đăng nhập trước khi gửi tin nhắn
        UUID assistantId = requireCurrentUserId();

        // Tìm phiên chat theo ID nhận từ header chat_session
        ChatSession chatSession = findChatSessionWithoutLock(chatSessionId);

        // Gán assistant cho phiên chat nếu phiên chưa có người phụ trách
        validateAssignedAssistant(chatSession, assistantId);
        validateChattingSession(chatSession);

        // Tạo tin nhắn assistant và lưu vào database
        ChatMessageResponseDTO chatMessage = createMessageResponse(
                chatSession,
                assistantId,
                ChatSenderType.ASSISTANT,
                content
        );

        // Gửi tin nhắn mới đến topic của phiên chat
        publishChatMessage(chatSession.getId(), chatMessage);

        // Gửi trạng thái phiên chat mới nhất đến danh sách assistant
        publishChatSessionUpdate(chatSession);

        // Trả về tin nhắn vừa tạo cho client WebSocket
        return chatMessage;
    }

    // Lấy hoặc tạo phiên chat hiện tại
    @Transactional
    public ChatSessionResponseDTO getOrCreateCurrentSessionResponse(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // Lấy hoặc tạo phiên chat hiện tại từ cookie chat_session
        ChatSession chatSession = getOrCreateCurrentSession(request, response);

        // Chuyển phiên chat sang DTO phản hồi
        return mapSessionResponse(chatSession);
    }

    // Gộp phiên chat hiện tại vào tài khoản đã đăng nhập
    @Transactional
    public ChatSessionResponseDTO mergeCurrentSession(HttpServletRequest request) {
        // Yêu cầu khách hàng phải đăng nhập trước khi gộp phiên chat
        UUID userId = requireCurrentUserId();

        // Lấy ID phiên chat hiện tại từ cookie chat_session
        String chatSessionId = getChatSessionIdFromCookieForMerge(request);
        // Kiểm tra cookie chat_session phải tồn tại
        if (!StringUtils.hasText(chatSessionId)) {
            throw new CustomException(404, "Không tìm thấy phiên chat để merge");
        }

        // Tìm phiên chat theo ID trong cookie, giữ phiên guest này làm phiên chính sau khi đăng nhập
        ChatSession chatSession = findChatSession(parseChatSessionId(chatSessionId));

        List<ChatSession> existingSessions = chatSessionRepository.findByUserIdAndStatusInOrderByUpdatedAtDesc(
                userId,
                MERGE_SESSION_STATUSES
        );

        // Gán userId hiện tại vào phiên chat
        chatSession.setUserId(userId);

        // Lưu phiên chat đã được gộp vào tài khoản
        ChatSession savedChatSession = chatSessionRepository.save(chatSession);

        existingSessions.stream()
                .filter(existingSession -> !existingSession.getId().equals(savedChatSession.getId()))
                .forEach(existingSession -> mergeDuplicateSessionIntoPrimary(existingSession, savedChatSession));

        publishChatSessionUpdate(savedChatSession);

        // Chuyển phiên chat sang DTO phản hồi
        return mapSessionResponse(savedChatSession);
    }

    // Lấy lịch sử tin nhắn của phiên chat hiện tại
    public List<ChatMessageResponseDTO> getCurrentSessionMessages(HttpServletRequest request) {
        // Tìm phiên chat hiện tại từ cookie chat_session
        ChatSession chatSession = findCurrentSession(request);

        // Lấy danh sách tin nhắn của phiên chat hiện tại
        return getSessionMessages(chatSession.getId());
    }

    // Lấy danh sách phiên chat cho assistant theo trạng thái
    public List<ChatSessionResponseDTO> getAssistantSessions(ChatSessionStatus chatSessionStatus) {
        // Tìm danh sách phiên chat theo trạng thái và sắp xếp mới nhất trước
        return chatSessionRepository.findByStatusOrderByUpdatedAtDesc(chatSessionStatus)
                .stream()
                .map(this::mapSessionResponse)
                .toList();
    }

    // Lấy lịch sử tin nhắn của một phiên chat cho assistant
    public List<ChatMessageResponseDTO> getAssistantSessionMessages(UUID chatSessionId) {
        // Lấy danh sách tin nhắn của phiên chat theo ID
        return getSessionMessages(chatSessionId);
    }

    // Nhận một phiên chat để hỗ trợ
    @Transactional
    public ChatSessionResponseDTO assignSession(UUID chatSessionId) {
        // Yêu cầu assistant phải đăng nhập trước khi nhận phiên chat
        UUID assistantId = requireCurrentUserId();

        // Tìm phiên chat theo ID
        ChatSession chatSession = findChatSessionWithLock(chatSessionId);
        if (chatSession.getStatus() != ChatSessionStatus.WAITING) {
            throw new CustomException(409, "Chat session is not waiting");
        }

        // Gán assistant hiện tại làm người phụ trách phiên chat
        chatSession.setAssignedAssistantId(assistantId);

        // Chuyển trạng thái phiên chat sang đang trò chuyện
        chatSession.setStatus(ChatSessionStatus.CHATTING);

        // Lưu phiên chat đã được gán assistant
        ChatSession savedChatSession = chatSessionRepository.save(chatSession);

        // Gửi trạng thái phiên chat mới nhất đến các topic liên quan
        publishChatSessionUpdate(savedChatSession);

        // Chuyển phiên chat sang DTO phản hồi
        return mapSessionResponse(savedChatSession);
    }

    // Đánh dấu phiên chat đã xử lý
    @Transactional
    public ChatSessionResponseDTO resolveSession(UUID chatSessionId) {
        UUID assistantId = requireCurrentUserId();
        // Tìm phiên chat theo ID
        ChatSession chatSession = findChatSessionWithoutLock(chatSessionId);
        validateAssignedAssistant(chatSession, assistantId);

        // Chuyển trạng thái phiên chat sang đã xử lý
        chatSession.setStatus(ChatSessionStatus.RESOLVED);

        // Lưu thời điểm xử lý xong phiên chat
        chatSession.setResolvedAt(Instant.now());

        // Lưu phiên chat đã được cập nhật trạng thái
        ChatSession savedChatSession = chatSessionRepository.save(chatSession);

        // Gửi trạng thái phiên chat mới nhất đến các topic liên quan
        publishChatSessionUpdate(savedChatSession);

        // Chuyển phiên chat sang DTO phản hồi
        return mapSessionResponse(savedChatSession);
    }

    // Đóng phiên chat
    @Transactional
    public ChatSessionResponseDTO closeSession(UUID chatSessionId) {
        UUID assistantId = requireCurrentUserId();
        // Tìm phiên chat theo ID
        ChatSession chatSession = findChatSessionWithoutLock(chatSessionId);
        validateAssignedAssistant(chatSession, assistantId);

        // Chuyển trạng thái phiên chat sang đã đóng
        chatSession.setStatus(ChatSessionStatus.CLOSED);

        // Lưu thời điểm đóng phiên chat
        chatSession.setClosedAt(Instant.now());

        // Lưu phiên chat đã được cập nhật trạng thái
        ChatSession savedChatSession = chatSessionRepository.save(chatSession);

        // Gửi trạng thái phiên chat mới nhất đến các topic liên quan
        publishChatSessionUpdate(savedChatSession);

        // Chuyển phiên chat sang DTO phản hồi
        return mapSessionResponse(savedChatSession);
    }

    // Lấy danh sách tin nhắn của một phiên chat
    private List<ChatMessageResponseDTO> getSessionMessages(UUID chatSessionId) {
        // Tìm danh sách tin nhắn theo phiên chat và sắp xếp từ cũ đến mới
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(chatSessionId)
                .stream()
                .map(chatMapper::toMessageResponse)
                .toList();
    }

    // Lấy hoặc tạo phiên chat hiện tại dựa trên cookie
    private ChatSession getOrCreateCurrentSession(HttpServletRequest request, HttpServletResponse response) {
        // Lấy ID phiên chat hiện tại từ cookie chat_session
        String chatSessionId = getChatSessionIdFromCookie(request);
        UUID currentUserId = getCurrentUserIdOrNull();

        if (currentUserId != null) {
            List<ChatSession> openSessions = chatSessionRepository.findByUserIdAndStatusInOrderByUpdatedAtDesc(
                    currentUserId,
                    OPEN_SESSION_STATUSES
            );

            if (!openSessions.isEmpty()) {
                ChatSession currentChatSession = openSessions.getFirst();
                openSessions.stream()
                        .skip(1)
                        .forEach(this::closeDuplicateOpenSession);
                setChatSessionCookie(response, currentChatSession);
                return currentChatSession;
            }
        }

        // Kiểm tra cookie chat_session có tồn tại hay không
        if (StringUtils.hasText(chatSessionId)) {
            // Tìm phiên chat hiện có theo ID trong cookie
            ChatSession existingChatSession = findChatSessionOrNull(chatSessionId);

            // Gia hạn cookie nếu phiên chat vẫn tồn tại
            if (existingChatSession != null) {
                setChatSessionCookie(response, existingChatSession);
                return existingChatSession;
            }
        }

        // Tạo phiên chat mới nếu chưa có cookie hợp lệ
        ChatSession createdChatSession = chatSessionRepository.save(ChatSession.builder()
                .userId(currentUserId)
                .guestId(UUID.randomUUID().toString())
                .status(ChatSessionStatus.WAITING)
                .build());

        // Lưu ID phiên chat mới vào cookie chat_session
        setChatSessionCookie(response, createdChatSession);

        // Trả về phiên chat mới tạo
        return createdChatSession;
    }

    // Tìm phiên chat hiện tại dựa trên cookie
    private ChatSession findCurrentSession(HttpServletRequest request) {
        // Lấy ID phiên chat hiện tại từ cookie chat_session
        String chatSessionId = getChatSessionIdFromCookie(request);

        // Kiểm tra cookie chat_session phải tồn tại
        if (!StringUtils.hasText(chatSessionId)) {
            throw new CustomException(404, "Không tìm thấy phiên chat");
        }

        // Tìm phiên chat theo ID trong cookie
        return findChatSession(parseChatSessionId(chatSessionId));
    }

    // Tìm phiên chat theo ID
    private ChatSession findChatSession(UUID chatSessionId) {
        // Tìm phiên chat trong database
        return chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new CustomException(404, "Không tìm thấy phiên chat"));
    }

    // Tìm phiên chat theo ID dạng chuỗi
    private ChatSession findChatSessionWithoutLock(UUID chatSessionId) {
        return findChatSession(chatSessionId);
    }

    private ChatSession findChatSessionWithLock(UUID chatSessionId) {
        return chatSessionRepository.findWithLockById(chatSessionId)
                .orElseThrow(() -> new CustomException(404, "KhÃ´ng tÃ¬m tháº¥y phiÃªn chat"));
    }

    private void validateAssignedAssistant(ChatSession chatSession, UUID assistantId) {
        if (!Objects.equals(chatSession.getAssignedAssistantId(), assistantId)) {
            throw new CustomException(403, "Chat session is assigned to another staff member");
        }
    }

    private void validateChattingSession(ChatSession chatSession) {
        if (chatSession.getStatus() != ChatSessionStatus.CHATTING) {
            throw new CustomException(409, "Chat session is not active");
        }
    }

    private void mergeDuplicateSessionIntoPrimary(ChatSession duplicateSession, ChatSession primarySession) {
        List<ChatMessage> duplicateMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(duplicateSession.getId());
        duplicateMessages.forEach(message -> message.setSession(primarySession));
        chatMessageRepository.saveAll(duplicateMessages);

        duplicateSession.setStatus(ChatSessionStatus.CLOSED);
        duplicateSession.setAssignedAssistantId(null);
        duplicateSession.setClosedAt(Instant.now());
        duplicateSession.setResolvedAt(null);
        chatSessionRepository.save(duplicateSession);
        publishChatSessionUpdate(duplicateSession);
    }

    private void closeDuplicateOpenSession(ChatSession chatSession) {
        chatSession.setStatus(ChatSessionStatus.CLOSED);
        chatSession.setClosedAt(Instant.now());
        chatSessionRepository.save(chatSession);
        publishChatSessionUpdate(chatSession);
    }

    private void setChatSessionCookie(HttpServletResponse response, ChatSession chatSession) {
        cookieUtil.setCookie(
                response,
                CookieUtil.CHAT_SESSION_COOKIE,
                chatSession.getId().toString(),
                CookieUtil.CHAT_SESSION_MAX_AGE_SEC
        );
    }

    private String getChatSessionIdFromCookieForMerge(HttpServletRequest request) {
        return getChatSessionIdFromCookie(request);
    }

    private ChatSession findChatSessionOrNull(String chatSessionId) {
        try {
            // Tìm phiên chat nếu chatSessionId là UUID hợp lệ
            return chatSessionRepository.findById(UUID.fromString(chatSessionId)).orElse(null);
        }
        catch (IllegalArgumentException ignored) {
            // Bỏ qua cookie không hợp lệ để tạo phiên chat mới
            return null;
        }
    }

    // Chuyển sessionId dạng chuỗi sang UUID
    private UUID parseChatSessionId(String chatSessionId) {
        try {
            // Chuyển chatSessionId sang UUID
            return UUID.fromString(chatSessionId);
        }
        catch (IllegalArgumentException e) {
            throw new CustomException(400, "Mã phiên chat không hợp lệ");
        }
    }

    // Lấy sessionId của phiên chat từ cookie
    private String getChatSessionIdFromCookie(HttpServletRequest request) {
        // Lấy giá trị cookie chat_session từ request
        return cookieUtil.getCookieValue(request, CookieUtil.CHAT_SESSION_COOKIE);
    }

    // Mở lại phiên chat nếu phiên đã xử lý hoặc đã đóng
    private ChatSession reopenSessionIfFinished(ChatSession chatSession) {
        // Bỏ qua nếu phiên chat vẫn đang chờ hoặc đang trò chuyện
        if (chatSession.getStatus() != ChatSessionStatus.RESOLVED && chatSession.getStatus() != ChatSessionStatus.CLOSED) {
            return chatSession;
        }

        // Chuyển trạng thái phiên chat về đang chờ hỗ trợ
        chatSession.setStatus(ChatSessionStatus.WAITING);

        // Bỏ nhân viên phụ trách cũ để bất kỳ nhân viên nào cũng có thể nhận lại phiên
        chatSession.setAssignedAssistantId(null);

        // Xóa thời điểm đóng phiên chat cũ
        chatSession.setClosedAt(null);

        // Xóa thời điểm xử lý phiên chat cũ
        chatSession.setResolvedAt(null);

        // Lưu phiên chat đã được mở lại
        return chatSessionRepository.save(chatSession);
    }

    // Tạo phản hồi tin nhắn sau khi lưu vào database
    private ChatMessageResponseDTO createMessageResponse(
            ChatSession chatSession,
            UUID senderId,
            ChatSenderType senderType,
            String content
    ) {
        // Kiểm tra nội dung tin nhắn không được để trống
        if (!StringUtils.hasText(content)) {
            throw new CustomException(400, "Nội dung tin nhắn không được để trống");
        }

        // Tạo tin nhắn mới qua mapper để service không tự dựng entity thủ công.
        ChatMessage chatMessage = chatMapper.toMessageEntity(chatSession, senderId, senderType, content);

        // Lưu tin nhắn mới vào database
        ChatMessage savedChatMessage = chatMessageRepository.save(chatMessage);

        // Chuyển tin nhắn sang DTO phản hồi
        return chatMapper.toMessageResponse(savedChatMessage);
    }

    // Gửi tin nhắn mới đến topic của phiên chat
    private void publishChatMessage(UUID chatSessionId, ChatMessageResponseDTO chatMessage) {
        // Gửi dữ liệu tin nhắn đến các client đang theo dõi phiên chat
        messagingTemplate.convertAndSend(CHAT_SESSION_TOPIC_PREFIX + chatSessionId, chatMessage);
    }

    // Gửi trạng thái phiên chat mới nhất đến các topic liên quan
    private void publishChatSessionUpdate(ChatSession chatSession) {
        // Chuyển phiên chat sang DTO phản hồi
        ChatSessionResponseDTO chatSessionResponse = mapSessionResponse(chatSession);

        // Gửi dữ liệu phiên chat đến client đang theo dõi phiên chat
        messagingTemplate.convertAndSend(CHAT_SESSION_TOPIC_PREFIX + chatSession.getId(), chatSessionResponse);

        // Gửi dữ liệu phiên chat đến danh sách assistant
        messagingTemplate.convertAndSend(ASSISTANT_CHAT_SESSIONS_TOPIC, chatSessionResponse);
    }

    private ChatSessionResponseDTO mapSessionResponse(ChatSession chatSession) {
        var lastMessageResult = chatMessageRepository.findFirstBySessionIdOrderByCreatedAtDesc(chatSession.getId());
        ChatMessage lastMessage = lastMessageResult == null ? null : lastMessageResult.orElse(null);

        return chatMapper.toSessionResponse(chatSession, lastMessage, resolveCustomerDisplayName(chatSession));
    }

    private String resolveCustomerDisplayName(ChatSession chatSession) {
        if (chatSession.getUserId() == null) {
            return GUEST_DISPLAY_NAME;
        }

        var contactResult = userProfileRepository.findContactByUserId(chatSession.getUserId());
        if (contactResult == null) {
            return GUEST_DISPLAY_NAME;
        }

        return contactResult
                .map(UserProfileRepository.UserContactView::getFullname)
                .filter(StringUtils::hasText)
                .orElse(GUEST_DISPLAY_NAME);
    }

    // Lấy userId hiện tại nếu người dùng đã đăng nhập
    private UUID getCurrentUserIdOrNull() {
        try {
            // Lấy userId hiện tại từ SecurityContext
            String userId = SecurityContextUtil.getCurrentUserId();

            // Chuyển userId sang UUID nếu có dữ liệu
            return StringUtils.hasText(userId) ? UUID.fromString(userId) : null;
        }
        catch (Exception ignored) {
            // Bỏ qua lỗi để tiếp tục xử lý như khách vãng lai
            return null;
        }
    }

    // Yêu cầu userId hiện tại để thực hiện thao tác cần đăng nhập
    private UUID requireCurrentUserId() {
        // Lấy userId hiện tại nếu có
        UUID userId = getCurrentUserIdOrNull();

        // Kiểm tra userId phải tồn tại
        if (userId == null) {
            throw new CustomException(401, "Bạn cần đăng nhập");
        }

        // Trả về userId hiện tại
        return userId;
    }
}
