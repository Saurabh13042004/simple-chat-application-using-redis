package com.chatapp.pubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Receives real-time messages broadcast via Redis Pub/Sub across all chat rooms.
 * Extend this class to fan messages out to WebSocket sessions or SSE streams.
 */
@Component
public class MessageSubscriber implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(MessageSubscriber.class);

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());
        logger.info("Real-time message received on channel [{}]: {}", channel, body);
    }
}
