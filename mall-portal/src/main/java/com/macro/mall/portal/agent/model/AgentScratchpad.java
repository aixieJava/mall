package com.macro.mall.portal.agent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 草稿纸：累积一次会话中各步骤的执行观察，供规划/反思/合成阶段读取。
 */
public class AgentScratchpad {

    private final String userGoal;
    private final List<AgentStep> steps = new ArrayList<>();

    public AgentScratchpad(String userGoal) {
        this.userGoal = userGoal;
    }

    public void record(AgentStep step) {
        steps.add(step);
    }

    public int iterationCount() {
        return steps.size();
    }

    public String getUserGoal() {
        return userGoal;
    }

    public List<AgentStep> getSteps() {
        return steps;
    }

    /** 渲染为给 LLM 的"已执行步骤与观察"上下文 */
    public String render() {
        if (steps.isEmpty()) {
            return "（暂无已执行步骤）";
        }
        StringBuilder sb = new StringBuilder();
        for (AgentStep s : steps) {
            sb.append("步骤").append(s.getIndex()).append("：").append(s.getGoal()).append("\n");
            sb.append("观察：").append(s.getObservation()).append("\n\n");
        }
        return sb.toString();
    }
}
