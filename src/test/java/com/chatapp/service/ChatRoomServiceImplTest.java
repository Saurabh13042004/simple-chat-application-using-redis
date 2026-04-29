package com.chatapp.service;

import com.chatapp.dto.request.CreateRoomRequest;
import com.chatapp.dto.request.JoinRoomRequest;
import com.chatapp.dto.request.SendMessageRequest;
import com.chatapp.dto.response.ChatHistoryResponse;
import com.chatapp.dto.response.CreateRoomResponse;
import com.chatapp.dto.response.DeleteRoomResponse;
import com.chatapp.dto.response.JoinRoomResponse;
import com.chatapp.dto.response.SendMessageResponse;
import com.chatapp.exception.ChatRoomNotFoundException;
import com.chatapp.exception.DuplicateChatRoomException;
import com.chatapp.model.ChatMessage;
import com.chatapp.pubsub.MessagePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatRoomServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private HashOperations<String, String, String> hashOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private MessagePublisher messagePublisher;

    private ChatRoomServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChatRoomServiceImpl(redisTemplate, messagePublisher, new ObjectMapper());
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        doReturn(setOperations).when(redisTemplate).opsForSet();
        doReturn(listOperations).when(redisTemplate).opsForList();
    }

    // --- createRoom ---

    @Test
    void createRoom_success_returnsRoomIdAndSuccessStatus() {
        when(redisTemplate.hasKey("chatroom:general")).thenReturn(false);

        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomName("general");

        CreateRoomResponse response = service.createRoom(request);

        assertThat(response.getRoomId()).isEqualTo("general");
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).contains("general");
        verify(hashOperations).putAll(eq("chatroom:general"), anyMap());
    }

    @Test
    void createRoom_duplicate_throwsDuplicateChatRoomException() {
        when(redisTemplate.hasKey("chatroom:general")).thenReturn(true);

        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomName("general");

        assertThatThrownBy(() -> service.createRoom(request))
                .isInstanceOf(DuplicateChatRoomException.class)
                .hasMessageContaining("general");
    }

    // --- joinRoom ---

    @Test
    void joinRoom_success_addsParticipantToSet() {
        when(redisTemplate.hasKey("chatroom:general")).thenReturn(true);

        JoinRoomRequest request = new JoinRoomRequest();
        request.setParticipant("guest_user");

        JoinRoomResponse response = service.joinRoom("general", request);

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).contains("guest_user").contains("general");
        verify(setOperations).add("chatroom:general:participants", "guest_user");
    }

    @Test
    void joinRoom_roomNotFound_throwsChatRoomNotFoundException() {
        when(redisTemplate.hasKey("chatroom:nonexistent")).thenReturn(false);

        JoinRoomRequest request = new JoinRoomRequest();
        request.setParticipant("guest_user");

        assertThatThrownBy(() -> service.joinRoom("nonexistent", request))
                .isInstanceOf(ChatRoomNotFoundException.class)
                .hasMessageContaining("nonexistent");
    }

    // --- sendMessage ---

    @Test
    void sendMessage_success_pushesToListAndPublishes() {
        when(redisTemplate.hasKey("chatroom:general")).thenReturn(true);

        SendMessageRequest request = new SendMessageRequest();
        request.setParticipant("guest_user");
        request.setMessage("Hello, everyone!");

        SendMessageResponse response = service.sendMessage("general", request);

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Message sent successfully.");

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(listOperations).rightPush(eq("chatroom:general:messages"), jsonCaptor.capture());
        assertThat(jsonCaptor.getValue()).contains("guest_user").contains("Hello, everyone!");
        verify(messagePublisher).publish(eq("general"), anyString());
    }

    @Test
    void sendMessage_roomNotFound_throwsChatRoomNotFoundException() {
        when(redisTemplate.hasKey("chatroom:nonexistent")).thenReturn(false);

        SendMessageRequest request = new SendMessageRequest();
        request.setParticipant("guest_user");
        request.setMessage("Hello!");

        assertThatThrownBy(() -> service.sendMessage("nonexistent", request))
                .isInstanceOf(ChatRoomNotFoundException.class)
                .hasMessageContaining("nonexistent");
    }

    // --- getMessages ---

    @Test
    void getMessages_success_returnsDeserializedMessages() throws Exception {
        when(redisTemplate.hasKey("chatroom:general")).thenReturn(true);
        when(listOperations.size("chatroom:general:messages")).thenReturn(2L);

        ObjectMapper mapper = new ObjectMapper();
        String msg1 = mapper.writeValueAsString(new ChatMessage("user1", "Hello", "2024-01-01T10:00:00Z"));
        String msg2 = mapper.writeValueAsString(new ChatMessage("user2", "Hi", "2024-01-01T10:01:00Z"));
        when(listOperations.range("chatroom:general:messages", 0L, -1L))
                .thenReturn(List.of(msg1, msg2));

        ChatHistoryResponse response = service.getMessages("general", 10);

        assertThat(response.getMessages()).hasSize(2);
        assertThat(response.getMessages().get(0).getParticipant()).isEqualTo("user1");
        assertThat(response.getMessages().get(1).getParticipant()).isEqualTo("user2");
    }

    @Test
    void getMessages_limitTrimsToLastN() throws Exception {
        when(redisTemplate.hasKey("chatroom:general")).thenReturn(true);
        when(listOperations.size("chatroom:general:messages")).thenReturn(5L);

        ObjectMapper mapper = new ObjectMapper();
        String msg = mapper.writeValueAsString(new ChatMessage("user1", "Last msg", "2024-01-01T10:04:00Z"));
        when(listOperations.range("chatroom:general:messages", 4L, -1L))
                .thenReturn(List.of(msg));

        ChatHistoryResponse response = service.getMessages("general", 1);

        assertThat(response.getMessages()).hasSize(1);
        assertThat(response.getMessages().get(0).getMessage()).isEqualTo("Last msg");
    }

    @Test
    void getMessages_emptyRoom_returnsEmptyList() {
        when(redisTemplate.hasKey("chatroom:general")).thenReturn(true);
        when(listOperations.size("chatroom:general:messages")).thenReturn(0L);
        when(listOperations.range("chatroom:general:messages", 0L, -1L)).thenReturn(List.of());

        ChatHistoryResponse response = service.getMessages("general", 10);

        assertThat(response.getMessages()).isEmpty();
    }

    @Test
    void getMessages_roomNotFound_throwsChatRoomNotFoundException() {
        when(redisTemplate.hasKey("chatroom:nonexistent")).thenReturn(false);

        assertThatThrownBy(() -> service.getMessages("nonexistent", 10))
                .isInstanceOf(ChatRoomNotFoundException.class)
                .hasMessageContaining("nonexistent");
    }

    // --- deleteRoom ---

    @Test
    void deleteRoom_success_deletesAllRoomKeys() {
        when(redisTemplate.hasKey("chatroom:general")).thenReturn(true);

        DeleteRoomResponse response = service.deleteRoom("general");

        assertThat(response.getStatus()).isEqualTo("success");
        verify(redisTemplate).delete("chatroom:general");
        verify(redisTemplate).delete("chatroom:general:participants");
        verify(redisTemplate).delete("chatroom:general:messages");
    }

    @Test
    void deleteRoom_roomNotFound_throwsChatRoomNotFoundException() {
        when(redisTemplate.hasKey("chatroom:nonexistent")).thenReturn(false);

        assertThatThrownBy(() -> service.deleteRoom("nonexistent"))
                .isInstanceOf(ChatRoomNotFoundException.class)
                .hasMessageContaining("nonexistent");
    }

    // --- Edge cases ---

    @Test
    void getMessages_nullListFromRedis_returnsEmptyList() {
        when(redisTemplate.hasKey("chatroom:general")).thenReturn(true);
        when(listOperations.size("chatroom:general:messages")).thenReturn(null);
        when(listOperations.range("chatroom:general:messages", 0L, -1L)).thenReturn(null);

        ChatHistoryResponse response = service.getMessages("general", 10);

        assertThat(response.getMessages()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendMessage_serializationFailure_throwsRuntimeException() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("err") {});

        ChatRoomServiceImpl failingService = new ChatRoomServiceImpl(redisTemplate, messagePublisher, mockMapper);
        when(redisTemplate.hasKey("chatroom:general")).thenReturn(true);

        SendMessageRequest request = new SendMessageRequest();
        request.setParticipant("user");
        request.setMessage("hello");

        assertThatThrownBy(() -> failingService.sendMessage("general", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to serialize message");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getMessages_deserializationFailure_throwsRuntimeException() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.readValue(anyString(), eq(ChatMessage.class)))
                .thenThrow(new JsonProcessingException("err") {});

        ChatRoomServiceImpl failingService = new ChatRoomServiceImpl(redisTemplate, messagePublisher, mockMapper);
        when(redisTemplate.hasKey("chatroom:general")).thenReturn(true);
        when(listOperations.size("chatroom:general:messages")).thenReturn(1L);
        when(listOperations.range("chatroom:general:messages", 0L, -1L))
                .thenReturn(List.of("{\"participant\":\"user\"}"));

        assertThatThrownBy(() -> failingService.getMessages("general", 10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to deserialize message");
    }
}
