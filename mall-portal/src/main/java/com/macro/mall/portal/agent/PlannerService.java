package com.macro.mall.portal.agent;

import com.macro.mall.portal.agent.model.AgentPlan;
import com.macro.mall.portal.agent.model.AgentScratchpad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * 规划器：把用户目标拆解为结构化执行计划。不挂工具、不挂记忆，保证可复现、可打日志。
 */
@Component
public class PlannerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlannerService.class);

    private static final String PLANNER_SYSTEM = """
            你是mall商城智能客服的规划器。你的职责是把用户请求拆解为可执行计划，而不是直接回答用户。

            判断原则：
            - 闲聊、问候、或仅需常识/知识库即可回答的简单问题，将 directAnswer 设为 true，并在 directReply 给出回答草稿。
            - 需要查询真实数据或多步处理的请求，将 directAnswer 设为 false，并给出 steps（控制在1到3步）。
            - 涉及退换货政策、配送规则、支付方式、会员权益、常见问题等知识类内容的步骤，needKnowledge 设为 true。
            - 涉及订单的步骤必须先确认用户已登录；可用工具见下。

            可用工具：
            %s

            严格输出符合给定结构的 JSON，不要使用 markdown 代码块包裹。
            """;

    private final ChatClient plannerClient;
    private final AgentToolRegistry toolRegistry;

    public PlannerService(ChatModel chatModel, AgentToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
        this.plannerClient = ChatClient.builder(chatModel)
                .defaultSystem(String.format(PLANNER_SYSTEM, toolRegistry.toolCatalog()))
                .build();
    }

    public AgentPlan plan(String goal) {
        try {
            AgentPlan plan = plannerClient.prompt()
                    .user("用户请求：" + goal)
                    .call()
                    .entity(AgentPlan.class);
            LOGGER.info("Planner 规划结果: directAnswer={} 步骤数={}",
                    plan != null && plan.isDirectAnswer(),
                    plan != null && plan.getSteps() != null ? plan.getSteps().size() : 0);
            return plan != null ? plan : fallback(goal);
        } catch (Exception e) {
            LOGGER.warn("Planner 解析失败，回退为直接回答: {}", e.getMessage());
            return fallback(goal);
        }
    }

    public AgentPlan replan(String goal, AgentScratchpad pad) {
        try {
            return plannerClient.prompt()
                    .user("用户请求：" + goal + "\n\n已执行步骤与观察：\n" + pad.render()
                            + "\n请基于已有进展给出后续计划。")
                    .call()
                    .entity(AgentPlan.class);
        } catch (Exception e) {
            LOGGER.warn("Planner 重规划失败: {}", e.getMessage());
            return fallback(goal);
        }
    }

    private AgentPlan fallback(String goal) {
        AgentPlan plan = new AgentPlan();
        plan.setDirectAnswer(false);
        AgentPlan.PlanStep step = new AgentPlan.PlanStep();
        step.setGoal(goal);
        step.setNeedKnowledge(true);
        plan.getSteps().add(step);
        return plan;
    }
}
