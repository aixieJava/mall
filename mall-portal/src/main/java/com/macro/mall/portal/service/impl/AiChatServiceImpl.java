package com.macro.mall.portal.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macro.mall.common.service.RedisService;
import com.macro.mall.portal.domain.ChatRequest;
import com.macro.mall.portal.domain.ChatResponse;
import com.macro.mall.portal.service.AiChatService;
import com.macro.mall.portal.service.impl.AiModelClient.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * AI 客服核心实现：对话管理 + Function Calling
 */
@Service
public class AiChatServiceImpl implements AiChatService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_HISTORY = 10;
    private static final int HISTORY_TTL_HOURS = 24;

    @Autowired
    private AiModelClient aiModelClient;
    @Autowired
    private FunctionToolExecutor functionToolExecutor;
    @Autowired
    private RedisService redisService;
    @Value("${redis.database}")
    private String REDIS_DATABASE;
    @Value("${redis.expire.common}")
    private Long REDIS_EXPIRE_COMMON;

    private static final String REDIS_KEY_CHAT_HISTORY = "ai:chat:history:";

    // ── System Prompt ──
    private static final String SYSTEM_PROMPT = """
            你是mall商城的AI智能客服，名叫"小Mall"。你的职责是帮助用户解决购物问题。

            你可以：
            1. 回答关于商品、订单、配送、退换货、支付等常见电商问题
            2. 根据用户需求搜索商品并推荐，推荐时列出名称、价格、卖点
            3. 帮助用户查询订单状态和物流信息

            规则：
            - 回答简洁友好，不超过200字
            - 商品推荐时结合用户需求做个性化建议
            - 订单查询需要用户先登录
            - 遇到无法处理的问题，引导用户联系人工客服
            - 不要编造商品信息，必须通过工具查询真实数据
            - 如果用户问商品相关问题，主动调用searchProducts搜索
            """;

    // ── 工具定义 ──
    private static final List<Tool> TOOLS = buildTools();

    private static List<Tool> buildTools() {
        List<Tool> tools = new ArrayList<>();

        Tool searchProducts = new Tool();
        searchProducts.setFunction(buildFunction("searchProducts", "搜索商城商品",
                Map.of("type", "object",
                        "properties", Map.of(
                                "keyword", Map.of("type", "string", "description", "搜索关键词"),
                                "sort", Map.of("type", "integer", "enum", List.of(1, 2, 3, 4),
                                        "description", "排序: 1新品 2销量 3价格升 4价格降")))));
        tools.add(searchProducts);

        Tool getDetail = new Tool();
        getDetail.setFunction(buildFunction("getProductDetail", "查询商品详情",
                Map.of("type", "object",
                        "properties", Map.of("productId", Map.of("type", "integer", "description", "商品ID")),
                        "required", List.of("productId"))));
        tools.add(getDetail);

        Tool listOrders = new Tool();
        listOrders.setFunction(buildFunction("listMyOrders", "查询用户订单列表",
                Map.of("type", "object",
                        "properties", Map.of("status", Map.of("type", "integer",
                                "description", "订单状态: 0待付款 1待发货 2已发货 3已完成 4已关闭，不传查全部")))));
        tools.add(listOrders);

        Tool queryOrder = new Tool();
        queryOrder.setFunction(buildFunction("queryOrder", "查询订单详情",
                Map.of("type", "object",
                        "properties", Map.of("orderId", Map.of("type", "integer", "description", "订单ID")),
                        "required", List.of("orderId"))));
        tools.add(queryOrder);

        return tools;
    }

    private static Tool.Function buildFunction(String name, String desc, Map<String, Object> params) {
        Tool.Function f = new Tool.Function();
        f.setName(name);
        f.setDescription(desc);
        f.setParameters(params);
        return f;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        // 1. 加载对话历史
        String sessionId = request.getSessionId();
        if (StrUtil.isBlank(sessionId)) {
            sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        String historyKey = REDIS_DATABASE + ":" + REDIS_KEY_CHAT_HISTORY + sessionId;
        List<Message> messages = loadHistory(historyKey);
        if (messages.isEmpty()) {
            messages.add(Message.system(SYSTEM_PROMPT));
        }

        // 2. 追加用户消息
        messages.add(Message.user(request.getMessage()));

        // 3. 调用 LLM
        ChatCompletionRequest llmReq = new ChatCompletionRequest();
        llmReq.setMessages(messages);
        llmReq.setTools(TOOLS);
        llmReq.setToolChoice("auto");

        ChatCompletionResponse llmResp = aiModelClient.chat(llmReq);
        ChatCompletionResponse.Choice choice = llmResp.getChoices().get(0);

        // 4. 判断是否需要工具调用
        if ("tool_calls".equals(choice.getFinishReason())) {
            Message assistantMsg = choice.getMessage();
            messages.add(assistantMsg);

            // 执行工具调用
            List<ChatResponse.ProductCard> productCards = null;
            ChatResponse.OrderCard orderCard = null;

            for (ToolCall tc : assistantMsg.getToolCalls()) {
                String funcName = tc.getFunction().getName();
                String arguments = tc.getFunction().getArguments();
                String toolResult = functionToolExecutor.execute(funcName, arguments);
                messages.add(Message.tool(tc.getId(), funcName, toolResult));

                // 提取前端展示用的卡片
                if (productCards == null) {
                    productCards = functionToolExecutor.extractProductCards(funcName, toolResult);
                }
                if (orderCard == null) {
                    orderCard = functionToolExecutor.extractOrderCard(funcName, toolResult);
                }
            }

            // 二次调用 LLM 汇总结果
            ChatCompletionRequest summaryReq = new ChatCompletionRequest();
            summaryReq.setMessages(messages);
            summaryReq.setTools(TOOLS);
            summaryReq.setToolChoice("auto");

            ChatCompletionResponse summaryResp = aiModelClient.chat(summaryReq);
            String reply = summaryResp.getChoices().get(0).getMessage().getContent();
            messages.add(Message.assistant(reply));

            // 保存历史
            saveHistory(historyKey, messages);

            // 组装响应
            ChatResponse response = new ChatResponse();
            response.setReply(reply);
            response.setSessionId(sessionId);
            response.setProducts(productCards);
            response.setOrder(orderCard);
            return response;

        } else {
            // 纯文本回复
            String reply = choice.getMessage().getContent();
            messages.add(Message.assistant(reply));
            saveHistory(historyKey, messages);

            ChatResponse response = new ChatResponse();
            response.setReply(reply);
            response.setSessionId(sessionId);
            return response;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Message> loadHistory(String key) {
        try {
            String json = (String) redisService.get(key);
            if (StrUtil.isNotBlank(json)) {
                List<Map<String, Object>> list = MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
                List<Message> messages = new ArrayList<>();
                // 第一条是 system prompt
                messages.add(Message.system(SYSTEM_PROMPT));
                // 只取最近 MAX_HISTORY 条（不含 system）
                int start = Math.max(1, list.size() - MAX_HISTORY * 2);
                for (int i = start; i < list.size(); i++) {
                    Map<String, Object> m = list.get(i);
                    Message msg = new Message();
                    msg.setRole((String) m.get("role"));
                    msg.setContent((String) m.get("content"));
                    messages.add(msg);
                }
                return messages;
            }
        } catch (Exception e) {
            LOGGER.warn("加载对话历史失败: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    private void saveHistory(String key, List<Message> messages) {
        try {
            // 只保存 user 和 assistant 消息（去掉 system 和 tool 消息）
            List<Map<String, String>> history = new ArrayList<>();
            for (Message m : messages) {
                if ("user".equals(m.getRole()) || "assistant".equals(m.getRole())) {
                    history.add(Map.of("role", m.getRole(), "content",
                            m.getContent() != null ? m.getContent() : ""));
                }
            }
            // 只保留最近 MAX_HISTORY * 2 条
            int start = Math.max(0, history.size() - MAX_HISTORY * 2);
            List<Map<String, String>> trimmed = history.subList(start, history.size());
            redisService.set(key, MAPPER.writeValueAsString(trimmed), REDIS_EXPIRE_COMMON);
        } catch (Exception e) {
            LOGGER.warn("保存对话历史失败: {}", e.getMessage());
        }
    }
}
