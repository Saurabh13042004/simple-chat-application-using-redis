package com.chatapp.dto.request;

import jakarta.validation.constraints.NotBlank;

public class JoinRoomRequest {

    @NotBlank(message = "Participant name must not be blank")
    private String participant;

    public String getParticipant() { return participant; }
    public void setParticipant(String participant) { this.participant = participant; }
}
