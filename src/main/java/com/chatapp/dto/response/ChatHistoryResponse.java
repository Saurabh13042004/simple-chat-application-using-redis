package com.chatapp.dto.response;

import com.chatapp.model.ChatMessage;

import java.util.List;

public class ChatHistoryResponse {

    private List<ChatMessage> messages;

    public ChatHistoryResponse(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public List<ChatMessage> getMessages() { return messages; }
}
