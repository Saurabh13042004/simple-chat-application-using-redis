package com.chatapp.dto.response;

public class SendMessageResponse {

    private String message;
    private String status;

    public SendMessageResponse(String message, String status) {
        this.message = message;
        this.status = status;
    }

    public String getMessage() { return message; }
    public String getStatus() { return status; }
}
