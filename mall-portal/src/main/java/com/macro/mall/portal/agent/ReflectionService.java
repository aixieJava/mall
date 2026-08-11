package com.macro.mall.portal.agent;

import com.macro.mall.portal.agent.model.AgentScratchpad;
import com.macro.mall.portal.agent.model.Reflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * 反思器：评估当前进展是否达成用户目标，决定继续、重规划或收尾。只评估不回答用户。
 */
@Component
public class ReflectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionService.class);

    private static final String REFLECT_SYSTEM = """
            你是mall商城智能客服的反思器。请基于用户目标和已执行步骤的观察，评估目标是否已达成。
            - 若已达成，goalAchieved 设为 true。
            - 若信息不足或方向有误，goalAchieved 设为 false，并在 nextInstruction 给出下一步具体指令；
              若需要彻底重新规划，needReplan 设为 true。
            你只做评估，不要直接回答用户。严格输出符合结构的 JSON，不要使用 markdown 代码块。
            """;

    private final ChatClient reflectClient;

    public ReflectionService(ChatModel chatModel) {
        this.reflectClient = ChatClient.builder(chatModel)
                .defaultSystem(REFLECT_SYSTEM)
                .build();
    }

    public Reflection reflect(String goal, AgentScratchpad pad) {
        try {
            Reflection r = reflectClient.prompt()
                    .user("用户目标：" + goal + "\n\n已执行步骤与观察：\n" + pad.render())
                    .call()
                    .entity(Reflection.class);
            if (r != null) {
                LOGGER.info("Reflect: goalAchieved={} needReplan={}", r.isGoalAchieved(), r.isNeedReplan());
                return r;
            }
        } catch (Exception e) {
            LOGGER.warn("Reflect 解析失败，默认目标已达成以收尾: {}", e.getMessage());
        }
        Reflection fallback = new Reflection();
        fallback.setGoalAchieved(true);
        return fallback;
    }
}
