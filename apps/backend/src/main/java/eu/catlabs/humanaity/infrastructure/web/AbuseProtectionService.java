package eu.catlabs.humanaity.infrastructure.web;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AbuseProtectionService {

    private final AbuseProtectionProperties properties;
    private final Clock clock;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();

    public AbuseProtectionService(AbuseProtectionProperties properties) {
        this.properties = properties;
        this.clock = Clock.systemUTC();
    }

    public void checkCityCreate(String subject) {
        checkAllowed("city-create", subject, properties.getCityCreate());
    }

    public void checkSimulationMutation(String subject) {
        checkAllowed("simulation-mutation", subject, properties.getSimulationMutation());
    }

    public void checkSimulationAssistant(String subject) {
        checkAllowed("simulation-assistant", subject, properties.getSimulationAssistant());
    }

    public void checkAgentChat(String subject) {
        checkAllowed("agent-chat", subject, properties.getAgentChat());
    }

    private void checkAllowed(String action, String subject, AbuseProtectionProperties.RateLimitPolicy policy) {
        if (policy == null || !policy.isEnabled() || policy.getLimit() <= 0 || policy.getWindowSeconds() <= 0) {
            return;
        }
        String actor = subject == null || subject.isBlank() ? "unknown" : subject;
        long window = Math.floorDiv(clock.instant().getEpochSecond(), policy.getWindowSeconds());
        Counter counter = counters.computeIfAbsent(action + ":" + actor, ignored -> new Counter());
        synchronized (counter) {
            if (counter.window != window) {
                counter.window = window;
                counter.count = 0;
            }
            if (counter.count >= policy.getLimit()) {
                throw new RateLimitExceededException("Too many " + action + " requests. Please retry later.");
            }
            counter.count += 1;
        }
    }

    private static final class Counter {
        private long window = Long.MIN_VALUE;
        private int count;
    }
}
