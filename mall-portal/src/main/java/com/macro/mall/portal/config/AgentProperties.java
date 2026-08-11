package com.macro.mall.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI Agent 编排配置（对应 application.yml 中 agent.* ）
 */
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    /** Plan-Act-Reflect 最大迭代轮数，防止死循环 */
    private int maxIterations = 4;

    private final Planner planner = new Planner();

    public static class Planner {
        /** false 时退化为旧单轮逻辑 */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public Planner getPlanner() {
        return planner;
    }
}
