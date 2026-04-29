package com.chatapp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatMessage {

    @JsonProperty("participant")
    private String participant;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private String timestamp;

    public ChatMessage() {}

    public ChatMessage(String participant, String message, String timestamp) {
        this.participant = participant;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getParticipant() { return participant; }
    public void setParticipant(String participant) { this.participant = participant; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
