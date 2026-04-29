package com.chatapp.controller;

import com.chatapp.dto.request.CreateRoomRequest;
import com.chatapp.dto.request.JoinRoomRequest;
import com.chatapp.dto.request.SendMessageRequest;
import com.chatapp.dto.response.ChatHistoryResponse;
import com.chatapp.dto.response.CreateRoomResponse;
import com.chatapp.dto.response.DeleteRoomResponse;
import com.chatapp.dto.response.JoinRoomResponse;
import com.chatapp.dto.response.SendMessageResponse;
import com.chatapp.service.ChatRoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatapp/chatrooms")
public class ChatController {

    private final ChatRoomService chatRoomService;

    public ChatController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateRoomResponse createRoom(@RequestBody @Valid CreateRoomRequest request) {
        return chatRoomService.createRoom(request);
    }

    @PostMapping("/{roomId}/join")
    public JoinRoomResponse joinRoom(@PathVariable String roomId,
                                     @RequestBody @Valid JoinRoomRequest request) {
        return chatRoomService.joinRoom(roomId, request);
    }

    @PostMapping("/{roomId}/messages")
    public SendMessageResponse sendMessage(@PathVariable String roomId,
                                           @RequestBody @Valid SendMessageRequest request) {
        return chatRoomService.sendMessage(roomId, request);
    }

    @GetMapping("/{roomId}/messages")
    public ChatHistoryResponse getMessages(@PathVariable String roomId,
                                           @RequestParam(defaultValue = "50") int limit) {
        return chatRoomService.getMessages(roomId, limit);
    }

    @DeleteMapping("/{roomId}")
    public DeleteRoomResponse deleteRoom(@PathVariable String roomId) {
        return chatRoomService.deleteRoom(roomId);
    }
}
