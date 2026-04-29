package com.chatapp.dto.request;

import jakarta.validation.constraints.NotBlank;

public class SendMessageRequest {

    @NotBlank(message = "Participant name must not be blank")
    private String participant;

    @NotBlank(message = "Message must not be blank")
    private String message;

    public String getParticipant() { return participant; }
    public void setParticipant(String participant) { this.participant = participant; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
