package eu.catlabs.humanaity.ai.application;

import eu.catlabs.humanaity.ai.domain.AiCallContextType;
import eu.catlabs.humanaity.ai.domain.AiCallLog;
import eu.catlabs.humanaity.ai.infrastructure.persistence.AiCallLogRepository;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ai-call-log-service;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AiCallLogServiceTest {

    @Autowired
    private AiCallLogService aiCallLogService;
    @Autowired
    private AiCallLogRepository aiCallLogRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        aiCallLogRepository.deleteAll();
        cityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void summarizeAndListReturnStableAggregatesAndOrdering() {
        City city = persistCity("Log City");

        AiCallLog first = aiCallLogRepository.save(log(city, "CHAT_FALLBACK", "OPENAI", "gpt-4", true, false, 10L, Instant.parse("2026-03-23T10:00:00Z")));
        AiCallLog second = aiCallLogRepository.save(log(city, "EVENT_ENRICHMENT", "OPENAI", "gpt-4", false, true, 20L, Instant.parse("2026-03-23T10:05:00Z")));
        AiCallLog third = aiCallLogRepository.save(log(city, "INVENTION_ENRICHMENT", "ANTHROPIC", "claude-3", true, false, 30L, Instant.parse("2026-03-23T10:05:00Z")));

        AiCallLogService.AiCallLogSummary summary = aiCallLogService.summarize(new AiCallLogFilter(null, null, null, null, null, null, null));
        assertThat(summary.totalCount()).isEqualTo(3L);
        assertThat(summary.successCount()).isEqualTo(2L);
        assertThat(summary.failureCount()).isEqualTo(1L);
        assertThat(summary.fallbackCount()).isEqualTo(1L);
        assertThat(summary.averageDurationMs()).isEqualTo(20L);
        assertThat(summary.latestRequestedAt()).isEqualTo(Instant.parse("2026-03-23T10:05:00Z"));
        assertThat(summary.byContextType()).extracting(AiCallLogService.AiCallLogContextSummary::contextType)
                .containsExactly(
                        AiCallContextType.CHAT_FALLBACK,
                        AiCallContextType.EVENT_ENRICHMENT,
                        AiCallContextType.INVENTION_ENRICHMENT
                );
        assertThat(summary.byProvider()).extracting(AiCallLogService.AiCallLogGroupSummary::key)
                .containsExactly("ANTHROPIC", "OPENAI");
        assertThat(summary.byModel()).extracting(AiCallLogService.AiCallLogGroupSummary::key)
                .containsExactly("claude-3", "gpt-4");

        List<AiCallLog> recent = aiCallLogService.list(new AiCallLogFilter(null, null, null, null, null, null, 2));
        assertThat(recent).extracting(AiCallLog::getId)
                .containsExactly(third.getId(), second.getId());
        assertThat(recent).extracting(AiCallLog::getRequestedAt)
                .containsExactly(Instant.parse("2026-03-23T10:05:00Z"), Instant.parse("2026-03-23T10:05:00Z"));
    }

    private City persistCity(String name) {
        User owner = new User();
        owner.setEmail(name.toLowerCase().replace(" ", "") + "@example.com");
        owner.setPassword("hash");
        owner.setRoles(Set.of("ROLE_USER"));
        owner = userRepository.save(owner);

        City city = new City();
        city.setName(name);
        city.setOwner(owner);
        return cityRepository.save(city);
    }

    private AiCallLog log(
            City city,
            String contextType,
            String provider,
            String model,
            boolean success,
            boolean fallbackUsed,
            long durationMs,
            Instant requestedAt
    ) {
        AiCallLog log = new AiCallLog();
        log.setCity(city);
        log.setContextType(AiCallContextType.valueOf(contextType));
        log.setContextEntityType("TEST");
        log.setContextEntityId(contextType);
        log.setProvider(provider);
        log.setModel(model);
        log.setSuccess(success);
        log.setFallbackUsed(fallbackUsed);
        log.setDurationMs(durationMs);
        log.setPromptSummary("Prompt");
        log.setResponseSummary(success ? "Response" : null);
        log.setPromptHash("abc");
        log.setResponseHash(success ? "def" : null);
        log.setErrorCode(success ? null : "TEST_ERROR");
        log.setErrorMessage(success ? null : "Failure");
        log.setRequestedAt(requestedAt);
        return log;
    }
}
