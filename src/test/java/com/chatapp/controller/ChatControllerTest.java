package com.chatapp.controller;

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
import com.chatapp.service.ChatRoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatRoomService chatRoomService;

    @Autowired
    private ObjectMapper objectMapper;

    // --- POST /api/chatapp/chatrooms ---

    @Test
    void createRoom_success_returns201WithRoomId() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomName("general");

        when(chatRoomService.createRoom(any(CreateRoomRequest.class)))
                .thenReturn(new CreateRoomResponse(
                        "Chat room 'general' created successfully.", "general", "success"));

        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.roomId").value("general"))
                .andExpect(jsonPath("$.message").value("Chat room 'general' created successfully."));
    }

    @Test
    void createRoom_duplicate_returns409() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomName("general");

        when(chatRoomService.createRoom(any(CreateRoomRequest.class)))
                .thenThrow(new DuplicateChatRoomException("Chat room 'general' already exists."));

        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Chat room 'general' already exists."));
    }

    @Test
    void createRoom_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    // --- POST /api/chatapp/chatrooms/{roomId}/join ---

    @Test
    void joinRoom_success_returns200() throws Exception {
        JoinRoomRequest request = new JoinRoomRequest();
        request.setParticipant("guest_user");

        when(chatRoomService.joinRoom(eq("general"), any(JoinRoomRequest.class)))
                .thenReturn(new JoinRoomResponse(
                        "User 'guest_user' joined chat room 'general'.", "success"));

        mockMvc.perform(post("/api/chatapp/chatrooms/general/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("User 'guest_user' joined chat room 'general'."));
    }

    @Test
    void joinRoom_roomNotFound_returns404() throws Exception {
        JoinRoomRequest request = new JoinRoomRequest();
        request.setParticipant("guest_user");

        when(chatRoomService.joinRoom(eq("nonexistent"), any(JoinRoomRequest.class)))
                .thenThrow(new ChatRoomNotFoundException("Chat room 'nonexistent' does not exist."));

        mockMvc.perform(post("/api/chatapp/chatrooms/nonexistent/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));
    }

    // --- POST /api/chatapp/chatrooms/{roomId}/messages ---

    @Test
    void sendMessage_success_returns200() throws Exception {
        SendMessageRequest request = new SendMessageRequest();
        request.setParticipant("guest_user");
        request.setMessage("Hello, everyone!");

        when(chatRoomService.sendMessage(eq("general"), any(SendMessageRequest.class)))
                .thenReturn(new SendMessageResponse("Message sent successfully.", "success"));

        mockMvc.perform(post("/api/chatapp/chatrooms/general/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Message sent successfully."));
    }

    @Test
    void sendMessage_roomNotFound_returns404() throws Exception {
        SendMessageRequest request = new SendMessageRequest();
        request.setParticipant("guest_user");
        request.setMessage("Hello!");

        when(chatRoomService.sendMessage(eq("nonexistent"), any(SendMessageRequest.class)))
                .thenThrow(new ChatRoomNotFoundException("Chat room 'nonexistent' does not exist."));

        mockMvc.perform(post("/api/chatapp/chatrooms/nonexistent/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void sendMessage_blankMessage_returns400() throws Exception {
        mockMvc.perform(post("/api/chatapp/chatrooms/general/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participant\":\"user\",\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    // --- GET /api/chatapp/chatrooms/{roomId}/messages ---

    @Test
    void getMessages_success_returnsMessageArray() throws Exception {
        List<ChatMessage> messages = List.of(
                new ChatMessage("guest_user", "Hello, everyone!", "2024-01-01T10:00:00Z"),
                new ChatMessage("another_user", "Hi, guest_user!", "2024-01-01T10:01:00Z")
        );

        when(chatRoomService.getMessages(eq("general"), eq(10)))
                .thenReturn(new ChatHistoryResponse(messages));

        mockMvc.perform(get("/api/chatapp/chatrooms/general/messages")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].participant").value("guest_user"))
                .andExpect(jsonPath("$.messages[0].message").value("Hello, everyone!"))
                .andExpect(jsonPath("$.messages[1].participant").value("another_user"));
    }

    @Test
    void getMessages_defaultLimit_uses50() throws Exception {
        when(chatRoomService.getMessages(eq("general"), eq(50)))
                .thenReturn(new ChatHistoryResponse(List.of()));

        mockMvc.perform(get("/api/chatapp/chatrooms/general/messages"))
                .andExpect(status().isOk());
    }

    @Test
    void getMessages_roomNotFound_returns404() throws Exception {
        when(chatRoomService.getMessages(eq("nonexistent"), anyInt()))
                .thenThrow(new ChatRoomNotFoundException("Chat room 'nonexistent' does not exist."));

        mockMvc.perform(get("/api/chatapp/chatrooms/nonexistent/messages"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));
    }

    // --- DELETE /api/chatapp/chatrooms/{roomId} ---

    @Test
    void deleteRoom_success_returns200() throws Exception {
        when(chatRoomService.deleteRoom("general"))
                .thenReturn(new DeleteRoomResponse(
                        "Chat room 'general' deleted successfully.", "success"));

        mockMvc.perform(delete("/api/chatapp/chatrooms/general"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Chat room 'general' deleted successfully."));
    }

    @Test
    void deleteRoom_roomNotFound_returns404() throws Exception {
        when(chatRoomService.deleteRoom("nonexistent"))
                .thenThrow(new ChatRoomNotFoundException("Chat room 'nonexistent' does not exist."));

        mockMvc.perform(delete("/api/chatapp/chatrooms/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));
    }

    // --- GlobalExceptionHandler catch-all ---

    @Test
    void anyEndpoint_unexpectedException_returns500() throws Exception {
        when(chatRoomService.deleteRoom("room"))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(delete("/api/chatapp/chatrooms/room"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred: Unexpected error"));
    }

    // --- Additional validation edge cases ---

    @Test
    void joinRoom_blankParticipant_returns400() throws Exception {
        mockMvc.perform(post("/api/chatapp/chatrooms/general/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participant\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void sendMessage_blankParticipant_returns400() throws Exception {
        mockMvc.perform(post("/api/chatapp/chatrooms/general/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participant\":\"\",\"message\":\"hello\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }
}
