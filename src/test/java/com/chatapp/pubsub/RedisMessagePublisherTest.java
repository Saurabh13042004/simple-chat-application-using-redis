package com.chatapp.pubsub;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedisMessagePublisherTest {

    @Test
    void publish_sendsMessageToCorrectChannel() {
        List<Object[]> calls = new ArrayList<>();
        RedisTemplate<String, String> fakeTemplate = new RedisTemplate<>() {
            @Override
            public Long convertAndSend(String channel, Object message) {
                calls.add(new Object[]{channel, message});
                return 1L;
            }
        };
        RedisMessagePublisher publisher = new RedisMessagePublisher(fakeTemplate);

        publisher.publish("general", "{\"participant\":\"alice\"}");

        assertThat(calls).hasSize(1);
        assertThat(calls.get(0)[0]).isEqualTo("chatroom:general");
        assertThat(calls.get(0)[1]).isEqualTo("{\"participant\":\"alice\"}");
    }

    @Test
    void publish_prefixesChannelWithChatroomColon() {
        List<Object[]> calls = new ArrayList<>();
        RedisTemplate<String, String> fakeTemplate = new RedisTemplate<>() {
            @Override
            public Long convertAndSend(String channel, Object message) {
                calls.add(new Object[]{channel, message});
                return 1L;
            }
        };
        RedisMessagePublisher publisher = new RedisMessagePublisher(fakeTemplate);

        publisher.publish("room-xyz", "payload");

        assertThat(calls).hasSize(1);
        assertThat(calls.get(0)[0]).isEqualTo("chatroom:room-xyz");
    }
}
