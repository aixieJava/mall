package com.macro.mall.portal.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macro.mall.common.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RedisChatMemory implements ChatMemory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisChatMemory.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String KEY_PREFIX = "ai:chat:history:";
    private static final int MAX_PAIRS = 10;

    @Autowired
    private RedisService redisService;
    @Value("${redis.database}")
    private String database;
    @Value("${redis.expire.common}")
    private Long expireSeconds;

    @Override
    public void add(String conversationId, List<Message> messages) {
        try {
            String key = database + ":" + KEY_PREFIX + conversationId;
            List<Map<String, String>> history = loadRaw(key);
            for (Message m : messages) {
                if (m instanceof UserMessage || m instanceof AssistantMessage) {
                    String content = m.getText() != null ? m.getText() : "";
                    history.add(Map.of("role", m instanceof UserMessage ? "user" : "assistant",
                                       "content", content));
                }
            }
            int start = Math.max(0, history.size() - MAX_PAIRS * 2);
            redisService.set(key,
                    MAPPER.writeValueAsString(history.subList(start, history.size())),
                    expireSeconds);
        } catch (Exception e) {
            LOGGER.warn("保存对话历史失败: {}", e.getMessage());
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        List<Map<String, String>> raw = loadRaw(database + ":" + KEY_PREFIX + conversationId);
        List<Message> result = new ArrayList<>();
        for (Map<String, String> m : raw) {
            String role = m.get("role");
            String content = m.getOrDefault("content", "");
            if ("user".equals(role)) {
                result.add(new UserMessage(content));
            } else if ("assistant".equals(role)) {
                result.add(new AssistantMessage(content));
            }
        }
        return result;
    }

    @Override
    public void clear(String conversationId) {
        redisService.del(database + ":" + KEY_PREFIX + conversationId);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> loadRaw(String key) {
        try {
            String json = (String) redisService.get(key);
            if (json != null && !json.isEmpty()) {
                return MAPPER.readValue(json, new TypeReference<List<Map<String, String>>>() {});
            }
        } catch (Exception e) {
            LOGGER.warn("加载对话历史失败: {}", e.getMessage());
        }
        return new ArrayList<>();
    }
}
