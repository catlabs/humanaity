package eu.catlabs.humanaity.infrastructure.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "humanaity.abuse-protection")
public class AbuseProtectionProperties {

    private RateLimitPolicy cityCreate = new RateLimitPolicy(3, 600);
    private RateLimitPolicy simulationMutation = new RateLimitPolicy(120, 60);
    private RateLimitPolicy simulationAssistant = new RateLimitPolicy(30, 60);
    private RateLimitPolicy agentChat = new RateLimitPolicy(20, 60);

    public RateLimitPolicy getCityCreate() {
        return cityCreate;
    }

    public void setCityCreate(RateLimitPolicy cityCreate) {
        this.cityCreate = cityCreate;
    }

    public RateLimitPolicy getSimulationMutation() {
        return simulationMutation;
    }

    public void setSimulationMutation(RateLimitPolicy simulationMutation) {
        this.simulationMutation = simulationMutation;
    }

    public RateLimitPolicy getSimulationAssistant() {
        return simulationAssistant;
    }

    public void setSimulationAssistant(RateLimitPolicy simulationAssistant) {
        this.simulationAssistant = simulationAssistant;
    }

    public RateLimitPolicy getAgentChat() {
        return agentChat;
    }

    public void setAgentChat(RateLimitPolicy agentChat) {
        this.agentChat = agentChat;
    }

    public static class RateLimitPolicy {
        private boolean enabled = true;
        private int limit;
        private long windowSeconds;

        public RateLimitPolicy() {
        }

        public RateLimitPolicy(int limit, long windowSeconds) {
            this.limit = limit;
            this.windowSeconds = windowSeconds;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public long getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(long windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }
}
