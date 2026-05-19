package org.example.p10.service.impl;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.example.p10.model.dto.ChatRequestDTO;
import org.example.p10.model.vo.ChatResponseVO;
import org.example.p10.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);
    private static final String REDIS_KEY_PREFIX = "chat:session:";
    private static final int MAX_HISTORY_ROUNDS = 3;

    private final StringRedisTemplate stringRedisTemplate;
    private final ChatClient chatClient;

    public ChatServiceImpl(ChatClient.Builder chatClientBuilder, StringRedisTemplate stringRedisTemplate) {
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一名专业、友好、简洁的中文智能助手，请结合历史上下文回答问题")
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .withTopP(0.7)
                                .build()
                )
                .build();
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public ChatResponseVO chat(ChatRequestDTO requestDTO) {
        String sessionId = requestDTO.getSessionId();
        String message = requestDTO.getMessage();

        // 校验参数
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }

        // 如果没有sessionId，直接调用模型不使用上下文
        if (sessionId == null || sessionId.trim().isEmpty()) {
            logger.info("未提供sessionId，进行单轮对话");
            String answer = chatClient.prompt(message).call().content();
            return new ChatResponseVO(message, answer);
        }

        String redisKey = REDIS_KEY_PREFIX + sessionId;
        logger.info("会话ID: {}, 用户消息: {}", sessionId, message);

        // 1. 读取历史消息
        List<String> records = stringRedisTemplate.opsForList().range(redisKey, 0, -1);
        StringBuilder historyText = new StringBuilder();
        if (records != null && !records.isEmpty()) {
            for (String record : records) {
                historyText.append(record).append("\n");
            }
            logger.info("读取到历史记录 {} 条", records.size());
        }

        // 2. 拼接上下文
        String finalPrompt = String.format("""
                以下是历史对话：
                %s
                当前用户问题：
                %s
                """, historyText, message);

        // 3. 调用模型
        String answer = chatClient.prompt(finalPrompt).call().content();
        logger.info("模型回答: {}", answer);

        // 4. 保存本轮记录
        String recordText = "用户: " + message + "\n助手: " + answer;
        stringRedisTemplate.opsForList().rightPush(redisKey, recordText);

        // 5. 只保留最近3轮
        Long size = stringRedisTemplate.opsForList().size(redisKey);
        if (size != null && size > MAX_HISTORY_ROUNDS) {
            stringRedisTemplate.opsForList().trim(redisKey, size - MAX_HISTORY_ROUNDS, size - 1);
            logger.info("历史记录超过{}轮，已截断", MAX_HISTORY_ROUNDS);
        }

        return new ChatResponseVO(message, answer);
    }
}