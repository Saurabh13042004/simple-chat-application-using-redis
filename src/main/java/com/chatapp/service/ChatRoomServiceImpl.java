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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Redis data structure usage:
 *   Hash  chatroom:{roomId}              → room metadata (name, createdAt)
 *   Set   chatroom:{roomId}:participants → participant names
 *   List  chatroom:{roomId}:messages     → JSON-serialised ChatMessage entries
 *   Pub/Sub channel chatroom:{roomId}    → real-time broadcast
 */
@Service
public class ChatRoomServiceImpl implements ChatRoomService {

    private static final String ROOM_KEY         = "chatroom:%s";
    private static final String PARTICIPANTS_KEY = "chatroom:%s:participants";
    private static final String MESSAGES_KEY     = "chatroom:%s:messages";

    private final RedisTemplate<String, String> redisTemplate;
    private final MessagePublisher messagePublisher;
    private final ObjectMapper objectMapper;

    public ChatRoomServiceImpl(RedisTemplate<String, String> redisTemplate,
                               MessagePublisher messagePublisher,
                               ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.messagePublisher = messagePublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    public CreateRoomResponse createRoom(CreateRoomRequest request) {
        String roomId = request.getRoomName();
        String roomKey = String.format(ROOM_KEY, roomId);

        if (Boolean.TRUE.equals(redisTemplate.hasKey(roomKey))) {
            throw new DuplicateChatRoomException("Chat room '" + roomId + "' already exists.");
        }

        Map<String, String> roomData = new HashMap<>();
        roomData.put("roomName", request.getRoomName());
        roomData.put("createdAt", Instant.now().toString());
        redisTemplate.<String, String>opsForHash().putAll(roomKey, roomData);

        return new CreateRoomResponse(
                "Chat room '" + roomId + "' created successfully.", roomId, "success");
    }

    @Override
    public JoinRoomResponse joinRoom(String roomId, JoinRoomRequest request) {
        ensureRoomExists(roomId);
        redisTemplate.opsForSet().add(
                String.format(PARTICIPANTS_KEY, roomId), request.getParticipant());
        return new JoinRoomResponse(
                "User '" + request.getParticipant() + "' joined chat room '" + roomId + "'.",
                "success");
    }

    @Override
    public SendMessageResponse sendMessage(String roomId, SendMessageRequest request) {
        ensureRoomExists(roomId);

        ChatMessage chatMessage = new ChatMessage(
                request.getParticipant(), request.getMessage(), Instant.now().toString());
        try {
            String json = objectMapper.writeValueAsString(chatMessage);
            redisTemplate.opsForList().rightPush(String.format(MESSAGES_KEY, roomId), json);
            messagePublisher.publish(roomId, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize message", e);
        }

        return new SendMessageResponse("Message sent successfully.", "success");
    }

    @Override
    public ChatHistoryResponse getMessages(String roomId, int limit) {
        ensureRoomExists(roomId);

        String messagesKey = String.format(MESSAGES_KEY, roomId);
        Long size = redisTemplate.opsForList().size(messagesKey);
        long listSize = size != null ? size : 0L;
        long start = Math.max(0L, listSize - limit);

        List<String> raw = redisTemplate.opsForList().range(messagesKey, start, -1);
        List<ChatMessage> messages = raw == null
                ? List.of()
                : raw.stream()
                    .map(json -> {
                        try {
                            return objectMapper.readValue(json, ChatMessage.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException("Failed to deserialize message", e);
                        }
                    })
                    .collect(Collectors.toList());

        return new ChatHistoryResponse(messages);
    }

    @Override
    public DeleteRoomResponse deleteRoom(String roomId) {
        ensureRoomExists(roomId);
        redisTemplate.delete(String.format(ROOM_KEY, roomId));
        redisTemplate.delete(String.format(PARTICIPANTS_KEY, roomId));
        redisTemplate.delete(String.format(MESSAGES_KEY, roomId));
        return new DeleteRoomResponse(
                "Chat room '" + roomId + "' deleted successfully.", "success");
    }

    private void ensureRoomExists(String roomId) {
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(String.format(ROOM_KEY, roomId)))) {
            throw new ChatRoomNotFoundException("Chat room '" + roomId + "' does not exist.");
        }
    }
}
