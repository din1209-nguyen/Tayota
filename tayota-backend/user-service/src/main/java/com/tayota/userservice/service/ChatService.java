package com.tayota.userservice.service;

import com.tayota.userservice.dto.Response.ChatMessageResponseDTO;
import com.tayota.userservice.dto.Response.ChatSessionResponseDTO;
import com.tayota.userservice.entity.ChatMessage;
import com.tayota.userservice.entity.ChatSession;
import com.tayota.userservice.enums.ChatSenderType;
import com.tayota.userservice.enums.ChatSessionStatus;
import com.tayota.userservice.repository.ChatMessageRepository;
import com.tayota.userservice.repository.ChatSessionRepository;
import com.tayota.userservice.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
// Service để quản lý các phiên chat và tin nhắn, bao gồm tạo phiên chat mới, gửi tin nhắn, gán nhân viên hỗ trợ, giải quyết và đóng phiên chat
// Các phương thức trong service này sử dụng @Transactional để đảm bảo tính nhất quán của dữ liệu khi thực hiện các thao tác liên quan đến phiên chat và tin nhắn
public class ChatService {
    // Tên cookie được sử dụng để lưu trữ ID của phiên chat hiện tại trên trình duyệt của khách hàng
    private static final String CHAT_SESSION_COOKIE = "chat-session";

    // Các repository và tiện ích được inject thông qua constructor để quản lý dữ liệu và gửi thông báo qua WebSocket
    // ChatSessionRepository để quản lý các phiên chat
    private final ChatSessionRepository chatSessionRepository;
    // ChatMessageRepository để quản lý các tin nhắn trong phiên chat
    private final ChatMessageRepository chatMessageRepository;
    // CookieUtil để quản lý cookie liên quan đến phiên chat trên trình duyệt của khách hàng
    private final CookieUtil cookieUtil;
    // SimpMessagingTemplate để gửi thông báo real-time về các phiên chat và tin nhắn mới đến các client qua WebSocket
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    // Phương thức để khách hàng gửi tin nhắn trong phiên chat hiện tại, nếu không có phiên chat nào tồn tại sẽ tự động tạo mới
    public ChatMessageResponseDTO customerSendMessage(
            String content, // Nội dung tin nhắn mà khách hàng muốn gửi
            HttpServletRequest request, // Đối tượng HttpServletRequest để lấy thông tin về phiên chat hiện tại từ cookie
            HttpServletResponse response // Đối tượng HttpServletResponse để thiết lập cookie mới nếu cần khi tạo phiên chat mới
    ) {
        // Lấy hoặc tạo mới phiên chat hiện tại dựa trên cookie, nếu phiên chat đã tồn tại nhưng ở trạng thái RESOLVED hoặc CLOSED thì sẽ chuyển sang trạng thái WAITING để tiếp tục cuộc trò chuyện
        ChatSession session = getOrCreateCurrentSession(request, response);
        // Nếu phiên chat đã tồn tại nhưng ở trạng thái RESOLVED hoặc CLOSED thì sẽ chuyển sang trạng thái WAITING để tiếp tục cuộc trò chuyện
        if (session.getStatus() == ChatSessionStatus.RESOLVED || session.getStatus() == ChatSessionStatus.CLOSED) {
            session.setStatus(ChatSessionStatus.WAITING);// Chuyển trạng thái về WAITING để chờ nhân viên hỗ trợ tiếp tục cuộc trò chuyện
            session.setClosedAt(null);// Xóa thời gian đóng phiên chat nếu có
            session.setResolvedAt(null);// Xóa thời gian giải quyết phiên chat nếu có
            session = chatSessionRepository.save(session);// Lưu lại phiên chat đã được cập nhật trạng thái
        }

        // Tạo và lưu tin nhắn mới vào cơ sở dữ liệu, liên kết với phiên chat hiện tại và đánh dấu người gửi là khách hàng
        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()// Tạo tin nhắn mới với nội dung đã được trim để loại bỏ khoảng trắng thừa
                .session(session)// Liên kết tin nhắn với phiên chat hiện tại
                .senderId(getCurrentUserIdOrNull())// Lấy ID người gửi nếu khách đã đăng nhập, nếu chưa đăng nhập sẽ là null
                .senderType(ChatSenderType.CUSTOMER)// Đánh dấu loại người gửi là khách hàng
                .content(content.trim())// Nội dung tin nhắn đã được trim để loại bỏ khoảng trắng thừa
                .build());// Lưu tin nhắn mới vào cơ sở dữ liệu

        ChatMessageResponseDTO messageResponse = toMessageResponse(message);// Chuyển đổi thực thể ChatMessage thành DTO để trả về cho client
        ChatSessionResponseDTO sessionResponse = toSessionResponse(session);// Chuyển đổi thực thể ChatSession thành DTO để gửi thông báo cập nhật trạng thái phiên chat cho các client

        // Gửi thông báo real-time về tin nhắn mới và cập nhật trạng thái phiên chat đến các client qua WebSocket, giúp cập nhật giao diện người dùng một cách nhanh chóng và mượt mà
        messagingTemplate.convertAndSend("/topic/chat.sessions." + session.getId(), messageResponse);
        messagingTemplate.convertAndSend("/topic/staff.chat.sessions", sessionResponse);

        return messageResponse;// Trả về dữ liệu phản hồi của tin nhắn mới cho client đã gửi yêu cầu
    }

    @Transactional
    // Phương thức để nhân viên hỗ trợ gửi tin nhắn trong một phiên chat cụ thể, nếu phiên chat chưa có nhân viên được chỉ định thì sẽ tự động gán nhân viên đó vào phiên chat và chuyển trạng thái sang CHATTING
    public ChatMessageResponseDTO staffSendMessage(UUID sessionId, String content) {
        // Yêu cầu phải có ID người dùng của nhân viên hỗ trợ đang gửi tin nhắn, nếu không có sẽ ném ra lỗi yêu cầu đăng nhập
        UUID staffId = requireCurrentUserId();
        // Tìm kiếm phiên chat theo ID, nếu không tìm thấy sẽ ném ra lỗi không tìm thấy phiên chat
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên chat"));

        // Nếu phiên chat chưa có nhân viên được chỉ định thì sẽ tự động gán nhân viên đó vào phiên chat và chuyển trạng thái sang CHATTING để bắt đầu cuộc trò chuyện
        if (session.getAssignedStaffId() == null) {
            session.setAssignedStaffId(staffId);// Gán nhân viên hỗ trợ vào phiên chat
            session.setStatus(ChatSessionStatus.CHATTING);// Chuyển trạng thái phiên chat sang CHATTING để bắt đầu cuộc trò chuyện
            session = chatSessionRepository.save(session);// Lưu lại phiên chat đã được cập nhật thông tin nhân viên hỗ trợ và trạng thái
        }
        
        // Tạo và lưu tin nhắn mới vào cơ sở dữ liệu, liên kết với phiên chat cụ thể và đánh dấu người gửi là nhân viên hỗ trợ
        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()// Tạo tin nhắn mới với nội dung đã được trim để loại bỏ khoảng trắng thừa
                .session(session)// Liên kết tin nhắn với phiên chat cụ thể
                .senderId(staffId)// Lấy ID người gửi là nhân viên hỗ trợ đang gửi tin nhắn
                .senderType(ChatSenderType.STAFF)// Đánh dấu loại người gửi là nhân viên hỗ trợ
                .content(content.trim())// Nội dung tin nhắn đã được trim để loại bỏ khoảng trắng thừa
                .build());// Lưu tin nhắn mới vào cơ sở dữ liệu
        // Chuyển đổi thực thể ChatMessage thành DTO để trả về cho client đã gửi yêu cầu 
        ChatMessageResponseDTO messageResponse = toMessageResponse(message);
        // Chuyển đổi thực thể ChatSession thành DTO để gửi thông báo cập nhật trạng thái phiên chat cho các client
        messagingTemplate.convertAndSend("/topic/chat.sessions." + session.getId(), messageResponse);
        // Gửi thông báo cập nhật trạng thái phiên chat đến các client qua WebSocket, giúp cập nhật giao diện người dùng một cách nhanh chóng và mượt mà
        messagingTemplate.convertAndSend("/topic/staff.chat.sessions", toSessionResponse(session));

        return messageResponse; // Trả về dữ liệu phản hồi của tin nhắn mới cho client đã gửi yêu cầu
    }

    @Transactional
    // Phương thức để khách hàng lấy thông tin phiên chat hiện tại, nếu không có phiên chat nào tồn tại sẽ tự động tạo mới và trả về thông tin của phiên chat đó
    public ChatSessionResponseDTO getOrCreateCurrentSessionResponse(
            HttpServletRequest request,// Đối tượng HttpServletRequest để lấy thông tin về phiên chat hiện tại từ cookie
            HttpServletResponse response// Đối tượng HttpServletResponse để thiết lập cookie mới nếu cần khi tạo phiên chat mới
    ) {
        return toSessionResponse(getOrCreateCurrentSession(request, response));// Lấy hoặc tạo mới phiên chat hiện tại và chuyển đổi thành DTO để trả về cho client
    }

    @Transactional
    // Phương thức để khách hàng gộp phiên chat hiện tại với tài khoản người dùng đã đăng nhập, nếu có phiên chat nào tồn tại sẽ được cập nhật thông tin người dùng và trả về thông tin của phiên chat đó
    public ChatSessionResponseDTO mergeCurrentSession(HttpServletRequest request) {
        UUID userId = requireCurrentUserId();// Yêu cầu phải có ID người dùng của khách hàng đang đăng nhập, nếu không có sẽ ném ra lỗi yêu cầu đăng nhập
        String sessionId = cookieUtil.getCookieValue(request, CHAT_SESSION_COOKIE);// Lấy ID phiên chat hiện tại từ cookie

        if (sessionId == null) {// Nếu không tìm thấy ID phiên chat trong cookie, có thể do khách hàng chưa bắt đầu phiên chat nào hoặc cookie đã bị xóa, ném ra lỗi không tìm thấy phiên chat để gộp
            throw new RuntimeException("Không tìm thấy phiên chat để merge");
        }

        ChatSession session = chatSessionRepository.findById(UUID.fromString(sessionId))// Tìm kiếm phiên chat theo ID, nếu không tìm thấy sẽ ném ra lỗi phiên chat không tồn tại
                .orElseThrow(() -> new RuntimeException("Phiên chat không tồn tại"));

        session.setUserId(userId);// Cập nhật thông tin người dùng vào phiên chat để gộp phiên chat hiện tại với tài khoản người dùng đã đăng nhập
        return toSessionResponse(chatSessionRepository.save(session));// Lưu lại phiên chat đã được cập nhật thông tin người dùng và chuyển đổi thành DTO để trả về cho client
    }

    // Phương thức để khách hàng lấy danh sách tin nhắn trong phiên chat hiện tại, nếu không có phiên chat nào tồn tại sẽ ném ra lỗi không tìm thấy phiên chat
    // Các tin nhắn được sắp xếp theo thời gian tạo tăng dần để hiển thị theo thứ tự từ cũ đến mới
    // Phương thức này giúp khách hàng xem lại lịch sử trò chuyện trong phiên chat hiện tại, đồng thời đảm bảo rằng chỉ có thể truy cập vào tin nhắn của phiên chat mà họ đang tham gia thông qua cookie
    // Nếu khách hàng chưa bắt đầu phiên chat nào hoặc cookie đã bị xóa, sẽ ném ra lỗi không tìm thấy phiên chat để lấy tin nhắn
    // Nếu phiên chat tồn tại nhưng không thuộc về khách hàng hiện tại (dựa trên cookie) thì cũng sẽ ném ra lỗi không tìm thấy phiên chat để lấy tin nhắn, đảm bảo tính bảo mật và riêng tư của các phiên chat
    public List<ChatMessageResponseDTO> getCurrentSessionMessages(HttpServletRequest request) {
        ChatSession session = findCurrentSession(request);// Tìm kiếm phiên chat hiện tại dựa trên cookie, nếu không tìm thấy sẽ ném ra lỗi không tìm thấy phiên chat để lấy tin nhắn
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())
                .stream()
                .map(this::toMessageResponse)// Chuyển đổi từng thực thể ChatMessage thành DTO để trả về cho client
                .toList();
    }

    // Phương thức để nhân viên hỗ trợ lấy danh sách các phiên chat theo trạng thái, giúp nhân viên dễ dàng quản lý và theo dõi các phiên chat đang chờ hỗ trợ, đang trò chuyện, đã giải quyết hoặc đã đóng
    // Các phiên chat được sắp xếp theo thời gian cập nhật giảm dần để
    public List<ChatSessionResponseDTO> getStaffSessions(ChatSessionStatus status) {
        return chatSessionRepository.findByStatusOrderByUpdatedAtDesc(status)
                .stream()
                .map(this::toSessionResponse)
                .toList();
    }

    // Phương thức để nhân viên hỗ trợ lấy danh sách tin nhắn trong một phiên chat cụ thể, giúp nhân viên xem lại lịch sử trò chuyện của phiên chat đó để hỗ trợ khách hàng một cách hiệu quả hơn
    // Các tin nhắn được sắp xếp theo thời gian tạo tăng dần để hiển thị theo thứ tự từ cũ đến mới, giúp nhân viên dễ dàng theo dõi diễn biến cuộc trò chuyện
    public List<ChatMessageResponseDTO> getStaffSessionMessages(UUID sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    // Phương thức để nhân viên hỗ trợ gán một phiên chat cụ thể vào chính mình, giúp nhân viên nhận trách nhiệm hỗ trợ khách hàng trong phiên chat đó và chuyển trạng thái phiên chat sang CHATTING nếu chưa có nhân viên nào được chỉ định
    public ChatSessionResponseDTO assignSession(UUID sessionId) {
        UUID staffId = requireCurrentUserId();// Yêu cầu phải có ID người dùng của nhân viên hỗ trợ đang thực hiện thao tác gán phiên chat, nếu không có sẽ ném ra lỗi yêu cầu đăng nhập

        ChatSession session = chatSessionRepository.findById(sessionId)// Tìm kiếm phiên chat theo ID, nếu không tìm thấy sẽ ném ra lỗi không tìm thấy phiên chat
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên chat"));

        session.setAssignedStaffId(staffId);// Gán nhân viên hỗ trợ vào phiên chat
        session.setStatus(ChatSessionStatus.CHATTING);// Chuyển trạng thái phiên chat sang CHATTING để bắt đầu cuộc trò chuyện nếu chưa có nhân viên nào được chỉ định

        ChatSessionResponseDTO response = toSessionResponse(chatSessionRepository.save(session));// Lưu lại phiên chat đã được cập nhật thông tin nhân viên hỗ trợ và trạng thái, sau đó chuyển đổi thành DTO để trả về cho client
        messagingTemplate.convertAndSend("/topic/chat.sessions." + sessionId, response);// Gửi thông báo cập nhật trạng thái phiên chat đến các client qua WebSocket, giúp cập nhật giao diện người dùng một cách nhanh chóng và mượt mà
        messagingTemplate.convertAndSend("/topic/staff.chat.sessions", response);
        return response;
    }

    @Transactional
    // Phương thức để nhân viên hỗ trợ giải quyết một phiên chat cụ thể, giúp nhân viên đánh dấu phiên chat đó đã được giải quyết và chuyển trạng thái sang RESOLVED, đồng thời gửi thông báo cập nhật trạng thái phiên chat đến các client qua WebSocket
    public ChatSessionResponseDTO resolveSession(UUID sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)// Tìm kiếm phiên chat theo ID, nếu không tìm thấy sẽ ném ra lỗi không tìm thấy phiên chat
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên chat"));

        session.setStatus(ChatSessionStatus.RESOLVED);// Chuyển trạng thái phiên chat sang RESOLVED để đánh dấu phiên chat đã được giải quyết
        session.setResolvedAt(Instant.now());// Cập nhật thời gian giải quyết phiên chat là thời điểm hiện tại

        ChatSessionResponseDTO response = toSessionResponse(chatSessionRepository.save(session));// Lưu lại phiên chat đã được cập nhật trạng thái và thời gian giải quyết, sau đó chuyển đổi thành DTO để trả về cho client
        messagingTemplate.convertAndSend("/topic/chat.sessions." + sessionId, response);// Gửi thông báo cập nhật trạng thái phiên chat đến các client qua WebSocket, giúp cập nhật giao diện người dùng một cách nhanh chóng và mượt mà
        messagingTemplate.convertAndSend("/topic/staff.chat.sessions", response);
        return response;
    }

    @Transactional
    // Phương thức để nhân viên hỗ trợ đóng một phiên chat cụ thể, giúp nhân viên đánh dấu phiên chat đó đã được đóng và chuyển trạng thái sang CLOSED, đồng thời gửi thông báo cập nhật trạng thái phiên chat đến các client qua WebSocket
    public ChatSessionResponseDTO closeSession(UUID sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên chat"));

        session.setStatus(ChatSessionStatus.CLOSED);// Chuyển trạng thái phiên chat sang CLOSED để đánh dấu phiên chat đã được đóng
        session.setClosedAt(Instant.now());// Cập nhật thời gian đóng phiên chat là thời điểm hiện tại

        ChatSessionResponseDTO response = toSessionResponse(chatSessionRepository.save(session));// Lưu lại phiên chat đã được cập nhật trạng thái và thời gian đóng, sau đó chuyển đổi thành DTO để trả về cho client
        messagingTemplate.convertAndSend("/topic/chat.sessions." + sessionId, response);// Gửi thông báo cập nhật trạng thái phiên chat đến các client qua WebSocket, giúp cập nhật giao diện người dùng một cách nhanh chóng và mượt mà
        messagingTemplate.convertAndSend("/topic/staff.chat.sessions", response);
        return response;
    }

    // Các phương thức tiện ích để quản lý phiên chat và tin nhắn, bao gồm lấy hoặc tạo mới phiên chat hiện tại dựa trên cookie, tìm kiếm phiên chat hiện tại, lấy ID người dùng hiện tại từ context bảo mật, chuyển đổi thực thể thành DTO, v.v.
    private ChatSession getOrCreateCurrentSession(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = cookieUtil.getCookieValue(request, CHAT_SESSION_COOKIE);

        if (sessionId != null) {// Nếu tìm thấy sessionId trong cookie, cố gắng tìm kiếm phiên chat tương ứng trong cơ sở dữ liệu
            try {
                ChatSession existing = chatSessionRepository.findById(UUID.fromString(sessionId)).orElse(null);// Tìm kiếm phiên chat theo ID, nếu không tìm thấy sẽ trả về null
                if (existing != null) {// Nếu tìm thấy phiên chat tương ứng với sessionId trong cookie, thiết lập lại cookie để gia hạn thời gian sống của cookie và trả về phiên chat đó
                    cookieUtil.setChatSessionCookie(response, existing.getId().toString());// Thiết lập lại cookie để gia hạn thời gian sống của cookie, giúp duy trì trạng thái phiên chat liên tục trên trình duyệt của khách hàng
                    return existing;// Trả về phiên chat đã tìm thấy để tiếp tục cuộc trò chuyện hiện tại
                }
            } catch (IllegalArgumentException ignored) {// Nếu sessionId trong cookie không phải là một UUID hợp lệ, sẽ ném ra IllegalArgumentException, trong trường hợp này sẽ bỏ qua lỗi và tiếp tục tạo mới phiên chat, đảm bảo rằng cookie không hợp
            }
        }

        ChatSession created = chatSessionRepository.save(ChatSession.builder()// Tạo mới một phiên chat với thông tin người dùng và khách hàng, trạng thái ban đầu là WAITING để chờ nhân viên hỗ trợ tiếp tục cuộc trò chuyện
                .userId(getCurrentUserIdOrNull())// Lấy ID người dùng nếu khách đã đăng nhập, nếu chưa đăng nhập sẽ là null
                .guestId(UUID.randomUUID().toString())// Tạo một guestId duy nhất cho khách hàng, giúp nhận diện phiên chat của khách hàng ngay cả khi họ chưa đăng nhập
                .status(ChatSessionStatus.WAITING)// Trạng thái ban đầu của phiên chat là WAITING để chờ nhân viên hỗ trợ tiếp tục cuộc trò chuyện
                .build());// Lưu phiên chat mới vào cơ sở dữ liệu

        cookieUtil.setChatSessionCookie(response, created.getId().toString());// Thiết lập cookie với sessionId của phiên chat mới tạo, giúp duy trì trạng thái phiên chat liên tục trên trình duyệt của khách hàng
        return created;
    }

    // Phương thức tiện ích để tìm kiếm phiên chat hiện tại dựa trên sessionId trong cookie, nếu không tìm thấy sẽ ném ra lỗi không tìm thấy phiên chat
    private ChatSession findCurrentSession(HttpServletRequest request) {
        String sessionId = cookieUtil.getCookieValue(request, CHAT_SESSION_COOKIE);// Lấy sessionId của phiên chat hiện tại từ cookie, nếu không tìm thấy sẽ trả về null
        if (sessionId == null) {// Nếu không tìm thấy sessionId trong cookie, có thể do khách hàng chưa bắt đầu phiên chat nào hoặc cookie đã bị xóa, ném ra lỗi không tìm thấy phiên chat
            throw new RuntimeException("Không tìm thấy phiên chat");
        }

        return chatSessionRepository.findById(UUID.fromString(sessionId))//
                .orElseThrow(() -> new RuntimeException("Phiên chat không tồn tại"));
    }

    // Phương thức tiện ích để lấy ID người dùng hiện tại từ context bảo mật, nếu không có thông tin người dùng hoặc thông tin không hợp lệ sẽ trả về null, giúp xác định xem khách hàng đã đăng nhập hay chưa và lấy ID người dùng nếu có
    private UUID getCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();// Lấy thông tin xác thực của người dùng hiện tại từ context bảo mật, nếu không có sẽ trả về null
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        try {
            return UUID.fromString(authentication.getPrincipal().toString());// Cố gắng chuyển đổi thông tin người dùng thành UUID, nếu không hợp lệ sẽ ném ra IllegalArgumentException và trả về null
        } catch (Exception e) {
            return null;
        }
    }

    private UUID requireCurrentUserId() {// Phương thức tiện ích để lấy ID người dùng hiện tại từ context bảo mật, nếu không có thông tin người dùng hoặc thông tin không hợp lệ sẽ ném ra lỗi yêu cầu đăng nhập, giúp đảm bảo rằng chỉ những khách hàng đã đăng nhập mới có thể thực hiện các thao tác liên quan đến phiên chat và tin nhắn
        UUID userId = getCurrentUserIdOrNull();
        if (userId == null) {
            throw new RuntimeException("Bạn cần đăng nhập");
        }
        return userId;
    }

    // Phương thức tiện ích để chuyển đổi thực thể ChatMessage thành DTO ChatMessageResponseDTO, giúp chuẩn hóa dữ liệu phản hồi của tin nhắn khi trả về cho client
    private ChatMessageResponseDTO toMessageResponse(ChatMessage message) {
        return ChatMessageResponseDTO.builder()
                .id(message.getId())
                .sessionId(message.getSession().getId())
                .senderId(message.getSenderId())
                .senderType(message.getSenderType())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }

    // Phương thức tiện ích để chuyển đổi thực thể ChatSession thành DTO ChatSessionResponseDTO, giúp chuẩn hóa dữ liệu phản hồi của phiên chat khi trả về cho client
    private ChatSessionResponseDTO toSessionResponse(ChatSession session) {
        return ChatSessionResponseDTO.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .guestId(session.getGuestId())
                .assignedStaffId(session.getAssignedStaffId())
                .status(session.getStatus())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .closedAt(session.getClosedAt())
                .resolvedAt(session.getResolvedAt())
                .build();
    }

    @Transactional
public ChatMessageResponseDTO customerSendMessageBySessionId(UUID sessionId, String content) {
    ChatSession session = chatSessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên chat"));

    if (session.getStatus() == ChatSessionStatus.RESOLVED || session.getStatus() == ChatSessionStatus.CLOSED) {
        session.setStatus(ChatSessionStatus.WAITING);
        session.setClosedAt(null);
        session.setResolvedAt(null);
        session = chatSessionRepository.save(session);
    }

    // Tạo và lưu tin nhắn mới vào cơ sở dữ liệu, liên kết với phiên chat cụ thể và đánh dấu người gửi là khách hàng
    ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
            .session(session)
            .senderId(getCurrentUserIdOrNull())
            .senderType(ChatSenderType.CUSTOMER)
            .content(content.trim())
            .build());

        ChatMessageResponseDTO messageResponse = toMessageResponse(message);
        messagingTemplate.convertAndSend("/topic/chat.sessions." + session.getId(), messageResponse);
        messagingTemplate.convertAndSend("/topic/staff.chat.sessions", toSessionResponse(session));
        return messageResponse;
    }
}