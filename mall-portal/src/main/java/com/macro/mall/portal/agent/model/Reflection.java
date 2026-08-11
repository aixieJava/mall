package com.macro.mall.portal.agent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Reflect 阶段的结构化输出：对当前进展的自我评估。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Reflection {

    @JsonPropertyDescription("用户目标是否已达成")
    private boolean goalAchieved;

    @JsonPropertyDescription("是否需要重新规划")
    private boolean needReplan;

    @JsonPropertyDescription("若未达成，给出下一步应执行的具体指令")
    private String nextInstruction;

    public boolean isGoalAchieved() {
        return goalAchieved;
    }

    public void setGoalAchieved(boolean goalAchieved) {
        this.goalAchieved = goalAchieved;
    }

    public boolean isNeedReplan() {
        return needReplan;
    }

    public void setNeedReplan(boolean needReplan) {
        this.needReplan = needReplan;
    }

    public String getNextInstruction() {
        return nextInstruction;
    }

    public void setNextInstruction(String nextInstruction) {
        this.nextInstruction = nextInstruction;
    }
}
