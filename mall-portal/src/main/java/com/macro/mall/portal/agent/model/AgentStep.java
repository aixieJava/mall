package com.macro.mall.portal.agent.model;

/**
 * 一次 Act 的执行记录，进入 scratchpad 供后续步骤与反思参考。
 */
public class AgentStep {
    private int index;
    private String goal;
    private String observation;
    private boolean usedTools;

    public AgentStep(int index, String goal, String observation, boolean usedTools) {
        this.index = index;
        this.goal = goal;
        this.observation = observation;
        this.usedTools = usedTools;
    }

    public int getIndex() {
        return index;
    }

    public String getGoal() {
        return goal;
    }

    public String getObservation() {
        return observation;
    }

    public boolean isUsedTools() {
        return usedTools;
    }
}
