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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {
    private static final String CHAT_SESSION_TOPIC_PREFIX = "/topic/chat.sessions.";
    private static final String ASSISTANT_CHAT_SESSIONS_TOPIC = "/topic/assistant.chat.sessions";

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CookieUtil cookieUtil;
    private final SimpMessagingTemplate messagingTemplate;

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
        publishAssistantChatSession(chatSession);

        // Trả về tin nhắn vừa tạo cho client gọi API
        return chatMessage;
    }

    // Gửi tin nhắn khách hàng vào một phiên chat cụ thể
    @Transactional
    public ChatMessageResponseDTO customerSendMessageToSession(UUID chatSessionId, String content) {
        // Tìm phiên chat theo ID nhận từ header chat_session
        ChatSession chatSession = findChatSession(chatSessionId);

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
        publishAssistantChatSession(chatSession);

        // Trả về tin nhắn vừa tạo cho client WebSocket
        return chatMessage;
    }

    // Gửi tin nhắn assistant trong một phiên chat
    @Transactional
    public ChatMessageResponseDTO assistantSendMessage(UUID chatSessionId, String content) {
        // Yêu cầu assistant phải đăng nhập trước khi gửi tin nhắn
        UUID assistantId = requireCurrentUserId();

        // Tìm phiên chat theo ID nhận từ header chat_session
        ChatSession chatSession = findChatSession(chatSessionId);

        // Gán assistant cho phiên chat nếu phiên chưa có người phụ trách
        chatSession = assignSessionIfNeeded(chatSession, assistantId);

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
        publishAssistantChatSession(chatSession);

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
        return ChatMapper.toSessionResponse(chatSession);
    }

    // Gộp phiên chat hiện tại vào tài khoản đã đăng nhập
    @Transactional
    public ChatSessionResponseDTO mergeCurrentSession(HttpServletRequest request) {
        // Yêu cầu khách hàng phải đăng nhập trước khi gộp phiên chat
        UUID userId = requireCurrentUserId();

        // Lấy ID phiên chat hiện tại từ cookie chat_session
        String chatSessionId = getChatSessionIdFromCookie(request);

        // Kiểm tra cookie chat_session phải tồn tại
        if (!StringUtils.hasText(chatSessionId)) {
            throw new CustomException(404, "Không tìm thấy phiên chat để merge");
        }

        // Tìm phiên chat theo ID trong cookie
        ChatSession chatSession = findChatSession(parseChatSessionId(chatSessionId));

        // Gán userId hiện tại vào phiên chat
        chatSession.setUserId(userId);

        // Lưu phiên chat đã được gộp vào tài khoản
        ChatSession savedChatSession = chatSessionRepository.save(chatSession);

        // Chuyển phiên chat sang DTO phản hồi
        return ChatMapper.toSessionResponse(savedChatSession);
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
                .map(ChatMapper::toSessionResponse)
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
        ChatSession chatSession = findChatSession(chatSessionId);

        // Gán assistant hiện tại làm người phụ trách phiên chat
        chatSession.setAssignedAssistantId(assistantId);

        // Chuyển trạng thái phiên chat sang đang trò chuyện
        chatSession.setStatus(ChatSessionStatus.CHATTING);

        // Lưu phiên chat đã được gán assistant
        ChatSession savedChatSession = chatSessionRepository.save(chatSession);

        // Gửi trạng thái phiên chat mới nhất đến các topic liên quan
        publishChatSessionUpdate(savedChatSession);

        // Chuyển phiên chat sang DTO phản hồi
        return ChatMapper.toSessionResponse(savedChatSession);
    }

    // Đánh dấu phiên chat đã xử lý
    @Transactional
    public ChatSessionResponseDTO resolveSession(UUID chatSessionId) {
        // Tìm phiên chat theo ID
        ChatSession chatSession = findChatSession(chatSessionId);

        // Chuyển trạng thái phiên chat sang đã xử lý
        chatSession.setStatus(ChatSessionStatus.RESOLVED);

        // Lưu thời điểm xử lý xong phiên chat
        chatSession.setResolvedAt(Instant.now());

        // Lưu phiên chat đã được cập nhật trạng thái
        ChatSession savedChatSession = chatSessionRepository.save(chatSession);

        // Gửi trạng thái phiên chat mới nhất đến các topic liên quan
        publishChatSessionUpdate(savedChatSession);

        // Chuyển phiên chat sang DTO phản hồi
        return ChatMapper.toSessionResponse(savedChatSession);
    }

    // Đóng phiên chat
    @Transactional
    public ChatSessionResponseDTO closeSession(UUID chatSessionId) {
        // Tìm phiên chat theo ID
        ChatSession chatSession = findChatSession(chatSessionId);

        // Chuyển trạng thái phiên chat sang đã đóng
        chatSession.setStatus(ChatSessionStatus.CLOSED);

        // Lưu thời điểm đóng phiên chat
        chatSession.setClosedAt(Instant.now());

        // Lưu phiên chat đã được cập nhật trạng thái
        ChatSession savedChatSession = chatSessionRepository.save(chatSession);

        // Gửi trạng thái phiên chat mới nhất đến các topic liên quan
        publishChatSessionUpdate(savedChatSession);

        // Chuyển phiên chat sang DTO phản hồi
        return ChatMapper.toSessionResponse(savedChatSession);
    }

    // Lấy danh sách tin nhắn của một phiên chat
    private List<ChatMessageResponseDTO> getSessionMessages(UUID chatSessionId) {
        // Tìm danh sách tin nhắn theo phiên chat và sắp xếp từ cũ đến mới
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(chatSessionId)
                .stream()
                .map(ChatMapper::toMessageResponse)
                .toList();
    }

    // Lấy hoặc tạo phiên chat hiện tại dựa trên cookie
    private ChatSession getOrCreateCurrentSession(HttpServletRequest request, HttpServletResponse response) {
        // Lấy ID phiên chat hiện tại từ cookie chat_session
        String chatSessionId = getChatSessionIdFromCookie(request);

        // Kiểm tra cookie chat_session có tồn tại hay không
        if (StringUtils.hasText(chatSessionId)) {
            // Tìm phiên chat hiện có theo ID trong cookie
            ChatSession existingChatSession = findChatSessionOrNull(chatSessionId);

            // Gia hạn cookie nếu phiên chat vẫn tồn tại
            if (existingChatSession != null) {
                cookieUtil.setCookie(
                        response,
                        CookieUtil.CHAT_SESSION_COOKIE,
                        existingChatSession.getId().toString(),
                        CookieUtil.CHAT_SESSION_MAX_AGE_SEC
                );
                return existingChatSession;
            }
        }

        // Tạo phiên chat mới nếu chưa có cookie hợp lệ
        ChatSession createdChatSession = chatSessionRepository.save(ChatSession.builder()
                .userId(getCurrentUserIdOrNull())
                .guestId(UUID.randomUUID().toString())
                .status(ChatSessionStatus.WAITING)
                .build());

        // Lưu ID phiên chat mới vào cookie chat_session
        cookieUtil.setCookie(
                response,
                CookieUtil.CHAT_SESSION_COOKIE,
                createdChatSession.getId().toString(),
                CookieUtil.CHAT_SESSION_MAX_AGE_SEC
        );

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

        // Xóa thời điểm đóng phiên chat cũ
        chatSession.setClosedAt(null);

        // Xóa thời điểm xử lý phiên chat cũ
        chatSession.setResolvedAt(null);

        // Lưu phiên chat đã được mở lại
        return chatSessionRepository.save(chatSession);
    }

    // Gán assistant cho phiên chat nếu chưa có assistant phụ trách
    private ChatSession assignSessionIfNeeded(ChatSession chatSession, UUID assistantId) {
        // Bỏ qua nếu phiên chat đã có assistant phụ trách
        if (chatSession.getAssignedAssistantId() != null) {
            return chatSession;
        }

        // Gán assistant hiện tại vào phiên chat
        chatSession.setAssignedAssistantId(assistantId);

        // Chuyển trạng thái phiên chat sang đang trò chuyện
        chatSession.setStatus(ChatSessionStatus.CHATTING);

        // Lưu phiên chat đã được gán assistant
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

        // Tạo tin nhắn mới và liên kết với phiên chat
        ChatMessage chatMessage = ChatMessage.builder()
                .session(chatSession)
                .senderId(senderId)
                .senderType(senderType)
                .content(content.trim())
                .build();

        // Lưu tin nhắn mới vào database
        ChatMessage savedChatMessage = chatMessageRepository.save(chatMessage);

        // Chuyển tin nhắn sang DTO phản hồi
        return ChatMapper.toMessageResponse(savedChatMessage);
    }

    // Gửi tin nhắn mới đến topic của phiên chat
    private void publishChatMessage(UUID chatSessionId, ChatMessageResponseDTO chatMessage) {
        // Gửi dữ liệu tin nhắn đến các client đang theo dõi phiên chat
        messagingTemplate.convertAndSend(CHAT_SESSION_TOPIC_PREFIX + chatSessionId, chatMessage);
    }

    // Gửi trạng thái phiên chat mới nhất đến topic của assistant
    private void publishAssistantChatSession(ChatSession chatSession) {
        // Chuyển phiên chat sang DTO phản hồi
        ChatSessionResponseDTO chatSessionResponse = ChatMapper.toSessionResponse(chatSession);

        // Gửi dữ liệu phiên chat đến danh sách assistant
        messagingTemplate.convertAndSend(ASSISTANT_CHAT_SESSIONS_TOPIC, chatSessionResponse);
    }

    // Gửi trạng thái phiên chat mới nhất đến các topic liên quan
    private void publishChatSessionUpdate(ChatSession chatSession) {
        // Chuyển phiên chat sang DTO phản hồi
        ChatSessionResponseDTO chatSessionResponse = ChatMapper.toSessionResponse(chatSession);

        // Gửi dữ liệu phiên chat đến client đang theo dõi phiên chat
        messagingTemplate.convertAndSend(CHAT_SESSION_TOPIC_PREFIX + chatSession.getId(), chatSessionResponse);

        // Gửi dữ liệu phiên chat đến danh sách assistant
        messagingTemplate.convertAndSend(ASSISTANT_CHAT_SESSIONS_TOPIC, chatSessionResponse);
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
