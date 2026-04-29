package com.chatapp.dto.response;

public class CreateRoomResponse {

    private String message;
    private String roomId;
    private String status;

    public CreateRoomResponse(String message, String roomId, String status) {
        this.message = message;
        this.roomId = roomId;
        this.status = status;
    }

    public String getMessage() { return message; }
    public String getRoomId() { return roomId; }
    public String getStatus() { return status; }
}
