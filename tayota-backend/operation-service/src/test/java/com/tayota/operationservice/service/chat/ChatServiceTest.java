package com.tayota.operationservice.service.chat;

import com.tayota.operationservice.entity.chat.ChatMessage;
import com.tayota.operationservice.entity.chat.ChatSession;
import com.tayota.operationservice.enums.chat.ChatSenderType;
import com.tayota.operationservice.enums.chat.ChatSessionStatus;
import com.tayota.operationservice.exception.CustomException;
import com.tayota.operationservice.repository.chat.ChatMessageRepository;
import com.tayota.operationservice.repository.chat.ChatSessionRepository;
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

    private ChatService service() {
        return new ChatService(chatSessionRepository, chatMessageRepository, cookieUtil, messagingTemplate);
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
}
