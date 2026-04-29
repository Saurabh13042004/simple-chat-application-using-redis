package com.chatapp.pubsub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;

import static org.assertj.core.api.Assertions.assertThatCode;

class MessageSubscriberTest {

    private MessageSubscriber subscriber;

    @BeforeEach
    void setUp() {
        subscriber = new MessageSubscriber();
    }

    @Test
    void onMessage_processesChannelAndBodyWithoutThrowing() {
        Message message = message("chatroom:general", "{\"participant\":\"alice\",\"message\":\"hello\"}");

        assertThatCode(() -> subscriber.onMessage(message, "chatroom:*".getBytes()))
                .doesNotThrowAnyException();
    }

    @Test
    void onMessage_nullPattern_doesNotThrow() {
        Message message = message("chatroom:test", "body");

        assertThatCode(() -> subscriber.onMessage(message, null))
                .doesNotThrowAnyException();
    }

    private static Message message(String channel, String body) {
        return new Message() {
            @Override public byte[] getBody()    { return body.getBytes(); }
            @Override public byte[] getChannel() { return channel.getBytes(); }
        };
    }
}
