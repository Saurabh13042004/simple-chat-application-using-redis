package com.chatapp.dto.response;

public class DeleteRoomResponse {

    private String message;
    private String status;

    public DeleteRoomResponse(String message, String status) {
        this.message = message;
        this.status = status;
    }

    public String getMessage() { return message; }
    public String getStatus() { return status; }
}
