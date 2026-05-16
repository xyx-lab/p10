package org.example.p10.service.impl;

import org.example.p10.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final ChatClient chatClient;

    public ChatServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一名专业、友好、简洁的中文智能助手，请根据用户的问题进行回答。")
                .build();
    }

    @Override
    public String chat(String message) {
        try {
            logger.info("Received chat message: {}", message);
            String content = chatClient.prompt(message).call().content();
            logger.info("Response content: {}", content);
            return content;
        } catch (Exception e) {
            logger.error("Error calling chat service", e);
            throw new RuntimeException("聊天服务调用失败: " + e.getMessage(), e);
        }
    }
}