package com.macro.mall.portal.agent;

import com.macro.mall.portal.agent.model.AgentScratchpad;
import com.macro.mall.portal.agent.model.AgentStep;
import com.macro.mall.portal.config.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Act 执行器：Plan-Act-Reflect 中唯一真正调用工具 + RAG 的环节。
 * 每个步骤目标足够窄，让 Spring AI 内部 tool-loop 在单步内自然收敛。
 * 会话记忆由 AgentOrchestrator 统一管理（只保存真实的用户/助手轮次），此处仅以上下文形式读取。
 */
@Component
public class ActExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActExecutor.class);

    private static final String ACT_SYSTEM = """
            你是mall商城智能客服"小Mall"的执行单元。请只完成【当前步骤目标】，不要提前处理其它步骤。
            规则：
            - 需要数据时调用合适的工具，不要编造商品或订单信息。
            - 工具调用失败（如未登录）时，如实说明失败原因。
            - 输出简洁，作为后续合成回复的素材即可。

            历史对话：
            %s

            已执行步骤与观察：
            %s
            """;

    private final ChatClient actClient;
    private final RagAdvisorFactory ragFactory;
    private final AgentToolRegistry toolRegistry;
    private final RagProperties ragProps;

    public ActExecutor(ChatModel chatModel, RagAdvisorFactory ragFactory,
                       AgentToolRegistry toolRegistry, RagProperties ragProps) {
        this.actClient = ChatClient.builder(chatModel).build();
        this.ragFactory = ragFactory;
        this.toolRegistry = toolRegistry;
        this.ragProps = ragProps;
    }

    public AgentStep act(int index, String goal, boolean needKnowledge, AgentScratchpad pad,
                         String historyContext, Map<String, Object> captured) {
        String system = String.format(ACT_SYSTEM, historyContext, pad.render());

        ChatClient.ChatClientRequestSpec spec = actClient.prompt()
                .system(system)
                .user(goal)
                .toolCallbacks(toolRegistry.buildCallbacks(captured));

        if (needKnowledge && ragProps.isEnabled()) {
            spec = spec.advisors(ragFactory.retrievalAdvisor());
        }

        String observation;
        try {
            observation = spec.call().content();
        } catch (Exception e) {
            observation = "执行失败：" + e.getMessage();
            LOGGER.warn("Act 步骤{} 执行异常: {}", index, e.getMessage());
        }
        boolean usedTools = !captured.isEmpty();
        LOGGER.info("Act 步骤{} 目标=[{}] needKnowledge={} 观察={}", index, goal, needKnowledge, observation);
        return new AgentStep(index, goal, observation, usedTools);
    }
}
