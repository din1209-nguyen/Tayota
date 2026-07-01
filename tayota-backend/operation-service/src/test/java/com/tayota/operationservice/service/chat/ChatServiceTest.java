package com.tayota.operationservice.service.chat;

import com.tayota.operationservice.entity.chat.ChatMessage;
import com.tayota.operationservice.entity.chat.ChatSession;
import com.tayota.operationservice.enums.chat.ChatSenderType;
import com.tayota.operationservice.enums.chat.ChatSessionStatus;
import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.mapper.chat.ChatMapper;
import com.tayota.operationservice.repository.chat.ChatMessageRepository;
import com.tayota.operationservice.repository.chat.ChatSessionRepository;
import com.tayota.operationservice.repository.user.UserProfileRepository;
import com.tayota.operationservice.util.CookieUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    @Mock
    private ChatSessionRepository chatSessionRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private CookieUtil cookieUtil;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private UUID currentUserId;

    @BeforeEach
    void authenticateUser() {
        currentUserId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                currentUserId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentSessionReusesNewestOpenSessionForAuthenticatedUserAndClosesDuplicates() {
        ChatSession newest = session(UUID.randomUUID(), currentUserId, ChatSessionStatus.CHATTING, UUID.randomUUID());
        ChatSession duplicate = session(UUID.randomUUID(), currentUserId, ChatSessionStatus.WAITING, null);
        when(chatSessionRepository.findByUserIdAndStatusInOrderByUpdatedAtDesc(currentUserId, List.of(ChatSessionStatus.WAITING, ChatSessionStatus.CHATTING)))
                .thenReturn(List.of(newest, duplicate));
        when(chatSessionRepository.save(duplicate)).thenReturn(duplicate);

        var response = service().getOrCreateCurrentSessionResponse(new MockHttpServletRequest(), new MockHttpServletResponse());

        assertThat(response.getId()).isEqualTo(newest.getId());
        assertThat(duplicate.getStatus()).isEqualTo(ChatSessionStatus.CLOSED);
        verify(cookieUtil).setCookie(any(), org.mockito.ArgumentMatchers.eq(CookieUtil.CHAT_SESSION_COOKIE), org.mockito.ArgumentMatchers.eq(newest.getId().toString()), org.mockito.ArgumentMatchers.eq(CookieUtil.CHAT_SESSION_MAX_AGE_SEC));
    }

    @Test
    void getCurrentSessionAttachesGuestCookieSessionToAuthenticatedUser() {
        UUID sessionId = UUID.randomUUID();
        ChatSession guestSession = session(sessionId, null, ChatSessionStatus.WAITING, null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cookieUtil.getCookieValue(request, CookieUtil.CHAT_SESSION_COOKIE)).thenReturn(sessionId.toString());
        when(chatSessionRepository.findByUserIdAndStatusInOrderByUpdatedAtDesc(currentUserId, List.of(ChatSessionStatus.WAITING, ChatSessionStatus.CHATTING)))
                .thenReturn(List.of());
        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.of(guestSession));
        when(chatSessionRepository.save(guestSession)).thenReturn(guestSession);

        var result = service().getOrCreateCurrentSessionResponse(request, response);

        assertThat(result.getId()).isEqualTo(sessionId);
        assertThat(guestSession.getUserId()).isEqualTo(currentUserId);
        verify(cookieUtil).setCookie(any(), org.mockito.ArgumentMatchers.eq(CookieUtil.CHAT_SESSION_COOKIE), org.mockito.ArgumentMatchers.eq(sessionId.toString()), org.mockito.ArgumentMatchers.eq(CookieUtil.CHAT_SESSION_MAX_AGE_SEC));
    }

    @Test
    void assignSessionRejectsAlreadyAssignedSession() {
        UUID sessionId = UUID.randomUUID();
        ChatSession assigned = session(sessionId, UUID.randomUUID(), ChatSessionStatus.CHATTING, UUID.randomUUID());
        when(chatSessionRepository.findWithLockById(sessionId)).thenReturn(Optional.of(assigned));

        assertThatThrownBy(() -> service().assignSession(sessionId))
                .isInstanceOf(CustomException.class)
                .extracting("code")
                .isEqualTo(409);

        verify(chatSessionRepository, never()).save(any(ChatSession.class));
    }

    @Test
    void assignSessionCanClaimWaitingSessionWithStaleAssistant() {
        UUID sessionId = UUID.randomUUID();
        ChatSession waiting = session(sessionId, UUID.randomUUID(), ChatSessionStatus.WAITING, UUID.randomUUID());
        when(chatSessionRepository.findWithLockById(sessionId)).thenReturn(Optional.of(waiting));
        when(chatSessionRepository.save(waiting)).thenReturn(waiting);

        var result = service().assignSession(sessionId);

        assertThat(result.getStatus()).isEqualTo(ChatSessionStatus.CHATTING);
        assertThat(waiting.getAssignedAssistantId()).isEqualTo(currentUserId);
    }

    @Test
    void assistantCannotSendMessageToSessionAssignedToSomeoneElse() {
        UUID sessionId = UUID.randomUUID();
        ChatSession assigned = session(sessionId, UUID.randomUUID(), ChatSessionStatus.CHATTING, UUID.randomUUID());
        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.of(assigned));

        assertThatThrownBy(() -> service().assistantSendMessage(sessionId, "Hello"))
                .isInstanceOf(CustomException.class)
                .extracting("code")
                .isEqualTo(403);

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void assistantCanReadMessagesFromSessionAssignedToSomeoneElse() {
        UUID sessionId = UUID.randomUUID();
        ChatSession assigned = session(sessionId, UUID.randomUUID(), ChatSessionStatus.CHATTING, UUID.randomUUID());
        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID())
                .session(assigned)
                .senderType(ChatSenderType.CUSTOMER)
                .content("Hi")
                .createdAt(Instant.now())
                .build();
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).thenReturn(List.of(message));

        var messages = service().getAssistantSessionMessages(sessionId);

        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst().getContent()).isEqualTo("Hi");
    }

    @Test
    void customerMessageReopensClosedSessionAndClearsAssignedAssistant() {
        UUID sessionId = UUID.randomUUID();
        UUID assignedAssistantId = UUID.randomUUID();
        ChatSession closed = session(sessionId, currentUserId, ChatSessionStatus.CLOSED, assignedAssistantId);
        closed.setClosedAt(Instant.now());
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cookieUtil.getCookieValue(request, CookieUtil.CHAT_SESSION_COOKIE)).thenReturn(sessionId.toString());
        when(chatSessionRepository.findByUserIdAndStatusInOrderByUpdatedAtDesc(currentUserId, List.of(ChatSessionStatus.WAITING, ChatSessionStatus.CHATTING)))
                .thenReturn(List.of());
        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.of(closed));
        when(chatSessionRepository.save(closed)).thenReturn(closed);
        ChatMessage savedMessage = ChatMessage.builder()
                .id(UUID.randomUUID())
                .session(closed)
                .senderId(currentUserId)
                .senderType(ChatSenderType.CUSTOMER)
                .content("Tôi cần hỗ trợ tiếp")
                .createdAt(Instant.now())
                .build();
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);
        when(chatMessageRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId)).thenReturn(Optional.of(savedMessage));

        service().customerSendMessage("Tôi cần hỗ trợ tiếp", request, response);

        assertThat(closed.getStatus()).isEqualTo(ChatSessionStatus.WAITING);
        assertThat(closed.getAssignedAssistantId()).isNull();
        assertThat(closed.getResolvedAt()).isNull();
        assertThat(closed.getClosedAt()).isNull();
    }

    @Test
    void mergeCurrentSessionKeepsGuestSessionAsPrimaryAndClosesDuplicateUserSession() {
        UUID guestSessionId = UUID.randomUUID();
        UUID duplicateSessionId = UUID.randomUUID();
        ChatSession guestSession = session(guestSessionId, null, ChatSessionStatus.WAITING, null);
        ChatSession duplicateSession = session(duplicateSessionId, currentUserId, ChatSessionStatus.CHATTING, UUID.randomUUID());
        ChatMessage duplicateMessage = ChatMessage.builder()
                .id(UUID.randomUUID())
                .session(duplicateSession)
                .senderType(ChatSenderType.CUSTOMER)
                .content("Tin nhắn cũ")
                .createdAt(Instant.now())
                .build();
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(cookieUtil.getCookieValue(request, CookieUtil.CHAT_SESSION_COOKIE)).thenReturn(guestSessionId.toString());
        when(chatSessionRepository.findById(guestSessionId)).thenReturn(Optional.of(guestSession));
        when(chatSessionRepository.findByUserIdAndStatusInOrderByUpdatedAtDesc(currentUserId, List.of(ChatSessionStatus.WAITING, ChatSessionStatus.CHATTING, ChatSessionStatus.CLOSED)))
                .thenReturn(List.of(duplicateSession));
        when(chatSessionRepository.save(guestSession)).thenReturn(guestSession);
        when(chatSessionRepository.save(duplicateSession)).thenReturn(duplicateSession);
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(duplicateSessionId)).thenReturn(List.of(duplicateMessage));

        var result = service().mergeCurrentSession(request);

        assertThat(result.getId()).isEqualTo(guestSessionId);
        assertThat(guestSession.getUserId()).isEqualTo(currentUserId);
        assertThat(duplicateMessage.getSession()).isEqualTo(guestSession);
        assertThat(duplicateSession.getStatus()).isEqualTo(ChatSessionStatus.CLOSED);
        assertThat(duplicateSession.getAssignedAssistantId()).isNull();
    }

    @Test
    void sessionResponseContainsCustomerDisplayNameAndLastMessage() {
        UUID sessionId = UUID.randomUUID();
        ChatSession chatSession = session(sessionId, currentUserId, ChatSessionStatus.WAITING, null);
        ChatMessage lastMessage = ChatMessage.builder()
                .id(UUID.randomUUID())
                .session(chatSession)
                .senderType(ChatSenderType.CUSTOMER)
                .content("Tin mới nhất")
                .createdAt(Instant.now())
                .build();
        when(chatSessionRepository.findByStatusOrderByUpdatedAtDesc(ChatSessionStatus.WAITING)).thenReturn(List.of(chatSession));
        when(chatMessageRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId)).thenReturn(Optional.of(lastMessage));
        when(userProfileRepository.findContactByUserId(currentUserId)).thenReturn(Optional.of(contact("Nguyễn Văn A")));

        var result = service().getAssistantSessions(ChatSessionStatus.WAITING);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCustomerDisplayName()).isEqualTo("Nguyễn Văn A");
        assertThat(result.getFirst().getLastMessageContent()).isEqualTo("Tin mới nhất");
        assertThat(result.getFirst().getLastMessageSenderType()).isEqualTo(ChatSenderType.CUSTOMER);
    }

    private ChatService service() {
        return new ChatService(chatSessionRepository, chatMessageRepository, userProfileRepository, cookieUtil, messagingTemplate, new ChatMapper());
    }

    private ChatSession session(UUID id, UUID userId, ChatSessionStatus status, UUID assignedAssistantId) {
        return ChatSession.builder()
                .id(id)
                .userId(userId)
                .guestId(UUID.randomUUID().toString())
                .assignedAssistantId(assignedAssistantId)
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private UserProfileRepository.UserContactView contact(String fullname) {
        return new UserProfileRepository.UserContactView() {
            @Override
            public String getFullname() {
                return fullname;
            }

            @Override
            public String getEmail() {
                return "customer@example.com";
            }

            @Override
            public String getPhone() {
                return "0900000000";
            }
        };
    }
}
