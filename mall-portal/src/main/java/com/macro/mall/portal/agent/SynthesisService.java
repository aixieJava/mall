package com.macro.mall.portal.agent;

import com.macro.mall.portal.agent.model.AgentScratchpad;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * 合成器：把多步观察整合为面向用户的最终回复。沿用"小Mall"人设与回复规则。
 */
@Component
public class SynthesisService {

    private static final String SYNTHESIS_SYSTEM = """
            你是mall商城的AI智能客服，名叫"小Mall"。请基于【已执行步骤与观察】整合出给用户的最终回复。
            规则：
            - 回答简洁友好，不超过200字。
            - 只使用观察中的真实数据，不要编造商品或订单信息。
            - 商品推荐结合用户需求做个性化建议；订单查询需用户先登录。
            - 遇到无法处理的问题，引导用户联系人工客服。
            """;

    private final ChatClient synthClient;

    public SynthesisService(ChatModel chatModel) {
        this.synthClient = ChatClient.builder(chatModel)
                .defaultSystem(SYNTHESIS_SYSTEM)
                .build();
    }

    public String synthesize(String userMessage, AgentScratchpad pad, String historyContext) {
        return synthClient.prompt()
                .user("历史对话：\n" + historyContext
                        + "\n\n用户本次问题：" + userMessage
                        + "\n\n已执行步骤与观察：\n" + pad.render()
                        + "\n\n请整合以上信息，给出最终回复。")
                .call()
                .content();
    }
}
