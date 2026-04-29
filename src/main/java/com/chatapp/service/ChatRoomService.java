package com.chatapp.service;

import com.chatapp.dto.request.CreateRoomRequest;
import com.chatapp.dto.request.JoinRoomRequest;
import com.chatapp.dto.request.SendMessageRequest;
import com.chatapp.dto.response.ChatHistoryResponse;
import com.chatapp.dto.response.CreateRoomResponse;
import com.chatapp.dto.response.DeleteRoomResponse;
import com.chatapp.dto.response.JoinRoomResponse;
import com.chatapp.dto.response.SendMessageResponse;

public interface ChatRoomService {
    CreateRoomResponse createRoom(CreateRoomRequest request);
    JoinRoomResponse joinRoom(String roomId, JoinRoomRequest request);
    SendMessageResponse sendMessage(String roomId, SendMessageRequest request);
    ChatHistoryResponse getMessages(String roomId, int limit);
    DeleteRoomResponse deleteRoom(String roomId);
}
