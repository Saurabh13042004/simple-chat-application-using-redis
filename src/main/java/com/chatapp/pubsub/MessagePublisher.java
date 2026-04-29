package com.chatapp.pubsub;

public interface MessagePublisher {
    void publish(String roomId, String message);
}
