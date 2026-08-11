package com.macro.mall.portal.agent;

import cn.hutool.core.util.StrUtil;
import com.macro.mall.portal.agent.model.AgentPlan;
import com.macro.mall.portal.agent.model.AgentScratchpad;
import com.macro.mall.portal.agent.model.AgentStep;
import com.macro.mall.portal.agent.model.Reflection;
import com.macro.mall.portal.config.AgentProperties;
import com.macro.mall.portal.config.RedisChatMemory;
import com.macro.mall.portal.domain.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 编排核心：显式的 Plan -> Act -> Observe -> Reflect 闭环。
 * 会话记忆在此统一管理：仅持久化真实的用户问题与最终回复，避免中间步骤污染历史。
 */
@Service
public class AgentOrchestrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final PlannerService planner;
    private final ActExecutor actExecutor;
    private final ReflectionService reflection;
    private final SynthesisService synthesis;
    private final AgentToolRegistry toolRegistry;
    private final RedisChatMemory chatMemory;
    private final AgentProperties props;

    public AgentOrchestrator(PlannerService planner, ActExecutor actExecutor,
                             ReflectionService reflection, SynthesisService synthesis,
                             AgentToolRegistry toolRegistry, RedisChatMemory chatMemory,
                             AgentProperties props) {
        this.planner = planner;
        this.actExecutor = actExecutor;
        this.reflection = reflection;
        this.synthesis = synthesis;
        this.toolRegistry = toolRegistry;
        this.chatMemory = chatMemory;
        this.props = props;
    }

    public ChatResponse run(String userMessage, String sessionId) {
        Map<String, Object> captured = new HashMap<>();   // 请求级，杜绝并发串扰
        String history = renderHistory(sessionId);
        AgentScratchpad pad = new AgentScratchpad(userMessage);

        // 规划：planner 关闭或判定 directAnswer 时退化为单步，不进入反思循环
        List<AgentPlan.PlanStep> pending;
        boolean reflectLoop;
        if (!props.getPlanner().isEnabled()) {
            pending = new ArrayList<>(List.of(singleStep(userMessage)));
            reflectLoop = false;
        } else {
            AgentPlan plan = planner.plan(userMessage);
            if (plan.isDirectAnswer() || plan.getSteps() == null || plan.getSteps().isEmpty()) {
                pending = new ArrayList<>(List.of(singleStep(userMessage)));
                reflectLoop = false;
            } else {
                pending = new ArrayList<>(plan.getSteps());
                reflectLoop = true;
            }
        }

        int idx = 0;
        while (!pending.isEmpty() && pad.iterationCount() < props.getMaxIterations()) {
            AgentPlan.PlanStep step = pending.remove(0);
            idx++;
            AgentStep s = actExecutor.act(idx, step.getGoal(), step.isNeedKnowledge(), pad, history, captured);
            pad.record(s);

            if (!reflectLoop) {
                break;   // directAnswer / 单步：执行一次即收尾
            }

            Reflection r = reflection.reflect(userMessage, pad);
            if (r.isGoalAchieved()) {
                break;
            }
            if (r.isNeedReplan()) {
                AgentPlan rp = planner.replan(userMessage, pad);
                pending = new ArrayList<>(rp.getSteps());
            } else if (StrUtil.isNotBlank(r.getNextInstruction()) && pending.isEmpty()) {
                AgentPlan.PlanStep ns = new AgentPlan.PlanStep();
                ns.setGoal(r.getNextInstruction());
                pending.add(ns);
            }
        }

        String reply = synthesis.synthesize(userMessage, pad, history);

        // 仅保存真实的一轮对话
        chatMemory.add(sessionId, List.of(new UserMessage(userMessage), new AssistantMessage(reply)));

        ChatResponse response = new ChatResponse();
        response.setReply(reply);
        response.setSessionId(sessionId);
        response.setProducts(toolRegistry.extractProducts(captured));
        response.setOrder(toolRegistry.extractOrder(captured));
        return response;
    }

    private AgentPlan.PlanStep singleStep(String goal) {
        AgentPlan.PlanStep step = new AgentPlan.PlanStep();
        step.setGoal(goal);
        step.setNeedKnowledge(true);
        return step;
    }

    private String renderHistory(String sessionId) {
        try {
            List<Message> messages = chatMemory.get(sessionId);
            if (messages == null || messages.isEmpty()) {
                return "（无历史对话）";
            }
            StringBuilder sb = new StringBuilder();
            for (Message m : messages) {
                if (m instanceof UserMessage) {
                    sb.append("用户: ").append(m.getText()).append("\n");
                } else if (m instanceof AssistantMessage) {
                    sb.append("客服: ").append(m.getText()).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            LOGGER.warn("加载历史对话失败: {}", e.getMessage());
            return "（无历史对话）";
        }
    }
}
