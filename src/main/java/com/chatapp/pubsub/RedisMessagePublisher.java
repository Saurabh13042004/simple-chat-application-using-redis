package com.chatapp.pubsub;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisMessagePublisher implements MessagePublisher {

    private static final String CHANNEL_PREFIX = "chatroom:";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisMessagePublisher(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void publish(String roomId, String message) {
        redisTemplate.convertAndSend(CHANNEL_PREFIX + roomId, message);
    }
}
