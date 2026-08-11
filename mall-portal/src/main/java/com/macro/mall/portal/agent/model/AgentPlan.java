package com.macro.mall.portal.agent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.ArrayList;
import java.util.List;

/**
 * Planner 的结构化输出：执行计划。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentPlan {

    @JsonPropertyDescription("是否为简单问答，可直接回答而无需多步工具执行")
    private boolean directAnswer;

    @JsonPropertyDescription("当 directAnswer 为 true 时，给出直接答复草稿")
    private String directReply;

    @JsonPropertyDescription("执行步骤列表，按先后顺序排列，控制在1到3步")
    private List<PlanStep> steps = new ArrayList<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PlanStep {
        @JsonPropertyDescription("本步骤的目标，用自然语言描述")
        private String goal;

        @JsonPropertyDescription("建议使用的工具名：searchProducts/getProductDetail/listMyOrders/queryOrder；纯推理则留空")
        private String suggestedTool;

        @JsonPropertyDescription("本步骤是否需要先检索知识库（如退换货政策、配送、支付、会员、FAQ）")
        private boolean needKnowledge;

        public String getGoal() {
            return goal;
        }

        public void setGoal(String goal) {
            this.goal = goal;
        }

        public String getSuggestedTool() {
            return suggestedTool;
        }

        public void setSuggestedTool(String suggestedTool) {
            this.suggestedTool = suggestedTool;
        }

        public boolean isNeedKnowledge() {
            return needKnowledge;
        }

        public void setNeedKnowledge(boolean needKnowledge) {
            this.needKnowledge = needKnowledge;
        }
    }

    public boolean isDirectAnswer() {
        return directAnswer;
    }

    public void setDirectAnswer(boolean directAnswer) {
        this.directAnswer = directAnswer;
    }

    public String getDirectReply() {
        return directReply;
    }

    public void setDirectReply(String directReply) {
        this.directReply = directReply;
    }

    public List<PlanStep> getSteps() {
        return steps;
    }

    public void setSteps(List<PlanStep> steps) {
        this.steps = steps;
    }
}
