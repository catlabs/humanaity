package eu.catlabs.humanaity.ai.application;

import eu.catlabs.humanaity.ai.domain.AiCallContextType;
import eu.catlabs.humanaity.ai.domain.AiCallLog;
import eu.catlabs.humanaity.ai.domain.AiResponse;
import eu.catlabs.humanaity.ai.infrastructure.persistence.AiCallLogRepository;
import eu.catlabs.humanaity.ai.infrastructure.port.AiServiceException;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class AiCallLogService {

    private static final int PROMPT_SUMMARY_MAX_LENGTH = 500;
    private static final int RESPONSE_SUMMARY_MAX_LENGTH = 500;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    private final AiCallLogRepository aiCallLogRepository;
    private final CityRepository cityRepository;

    public AiCallLogService(AiCallLogRepository aiCallLogRepository, CityRepository cityRepository) {
        this.aiCallLogRepository = aiCallLogRepository;
        this.cityRepository = cityRepository;
    }

    @Transactional
    public AiCallLog recordSuccess(
            AiGenerationContext context,
            eu.catlabs.humanaity.ai.domain.AiPrompt prompt,
            String provider,
            String model,
            AiResponse response,
            long durationMs
    ) {
        AiCallLog log = new AiCallLog();
        log.setRequestedAt(Instant.now());
        log.setCity(resolveCity(context.cityId()));
        log.setContextType(defaultContextType(context));
        log.setContextEntityType(trimToLength(context.contextEntityType(), 64));
        log.setContextEntityId(trimToLength(context.contextEntityId(), 128));
        log.setProvider(trimToLength(provider, 32));
        log.setModel(trimToLength(model, 128));
        log.setSuccess(true);
        log.setFallbackUsed(false);
        log.setDurationMs(Math.max(durationMs, 0L));
        log.setPromptSummary(buildPromptSummary(context, prompt));
        log.setResponseSummary(buildResponseSummary(response));
        log.setPromptHash(hashPrompt(prompt));
        log.setResponseHash(hashText(response == null ? null : response.getRawContent()));
        log.setErrorCode(null);
        log.setErrorMessage(null);
        return aiCallLogRepository.save(log);
    }

    @Transactional
    public AiCallLog recordFailure(
            AiGenerationContext context,
            eu.catlabs.humanaity.ai.domain.AiPrompt prompt,
            String provider,
            String model,
            long durationMs,
            Exception exception
    ) {
        AiCallLog log = new AiCallLog();
        log.setRequestedAt(Instant.now());
        log.setCity(resolveCity(context.cityId()));
        log.setContextType(defaultContextType(context));
        log.setContextEntityType(trimToLength(context.contextEntityType(), 64));
        log.setContextEntityId(trimToLength(context.contextEntityId(), 128));
        log.setProvider(trimToLength(provider, 32));
        log.setModel(trimToLength(model, 128));
        log.setSuccess(false);
        log.setFallbackUsed(context.fallbackExpected());
        log.setDurationMs(Math.max(durationMs, 0L));
        log.setPromptSummary(buildPromptSummary(context, prompt));
        log.setResponseSummary(null);
        log.setPromptHash(hashPrompt(prompt));
        log.setResponseHash(null);
        log.setErrorCode(resolveErrorCode(exception));
        log.setErrorMessage(trimToLength(resolveErrorMessage(exception), ERROR_MESSAGE_MAX_LENGTH));
        return aiCallLogRepository.save(log);
    }

    @Transactional
    public void markFallbackUsed(Long logId) {
        if (logId == null) {
            return;
        }
        aiCallLogRepository.findById(logId).ifPresent(log -> {
            log.setFallbackUsed(true);
            aiCallLogRepository.save(log);
        });
    }

    @Transactional(readOnly = true)
    public List<AiCallLog> list(AiCallLogFilter filter) {
        AiCallLogFilter safeFilter = filter == null ? new AiCallLogFilter(null, null, null, null, null, null, null) : filter;
        int limit = safeFilter.limit() == null || safeFilter.limit() <= 0 ? 100 : Math.min(safeFilter.limit(), 500);
        return aiCallLogRepository.findAll(Sort.by(Sort.Order.desc("requestedAt"), Sort.Order.desc("id"))).stream()
                .filter(log -> matchesFilter(log, safeFilter))
                .limit(limit)
                .toList();
    }

    @Transactional(readOnly = true)
    public AiCallLogSummary summarize(AiCallLogFilter filter) {
        List<AiCallLog> logs = aiCallLogRepository.findAll(Sort.by(Sort.Order.desc("requestedAt"), Sort.Order.desc("id"))).stream()
                .filter(log -> matchesFilter(log, filter == null ? new AiCallLogFilter(null, null, null, null, null, null, null) : filter))
                .toList();

        List<AiCallLogContextSummary> byContextType = logs.stream()
                .collect(java.util.stream.Collectors.groupingBy(AiCallLog::getContextType))
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().name()))
                .map(entry -> {
                    List<AiCallLog> contextLogs = entry.getValue();
                    long successCount = contextLogs.stream().filter(AiCallLog::isSuccess).count();
                    long fallbackCount = contextLogs.stream().filter(AiCallLog::isFallbackUsed).count();
                    return new AiCallLogContextSummary(
                            entry.getKey(),
                            contextLogs.size(),
                            successCount,
                            contextLogs.size() - successCount,
                            fallbackCount
                    );
                })
                .toList();

        long successCount = logs.stream().filter(AiCallLog::isSuccess).count();
        long fallbackCount = logs.stream().filter(AiCallLog::isFallbackUsed).count();
        long averageDurationMs = logs.isEmpty()
                ? 0L
                : Math.round(logs.stream().mapToLong(AiCallLog::getDurationMs).average().orElse(0.0d));
        Instant latestRequestedAt = logs.stream()
                .map(AiCallLog::getRequestedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new AiCallLogSummary(
                logs.size(),
                successCount,
                logs.size() - successCount,
                fallbackCount,
                averageDurationMs,
                latestRequestedAt,
                byContextType,
                summarizeBreakdown(logs, AiCallLog::getProvider),
                summarizeBreakdown(logs, AiCallLog::getModel)
        );
    }

    private boolean matchesFilter(AiCallLog log, AiCallLogFilter filter) {
        if (filter.cityId() != null) {
            Long logCityId = log.getCity() == null ? null : log.getCity().getId();
            if (!Objects.equals(filter.cityId(), logCityId)) {
                return false;
            }
        }
        if (filter.contextType() != null && log.getContextType() != filter.contextType()) {
            return false;
        }
        if (filter.success() != null && filter.success() != log.isSuccess()) {
            return false;
        }
        if (filter.fallbackUsed() != null && filter.fallbackUsed() != log.isFallbackUsed()) {
            return false;
        }
        if (filter.provider() != null && !filter.provider().equalsIgnoreCase(nullToEmpty(log.getProvider()))) {
            return false;
        }
        if (filter.model() != null && !filter.model().equalsIgnoreCase(nullToEmpty(log.getModel()))) {
            return false;
        }
        return true;
    }

    private City resolveCity(Long cityId) {
        if (cityId == null) {
            return null;
        }
        return cityRepository.findById(cityId).orElse(null);
    }

    private AiCallContextType defaultContextType(AiGenerationContext context) {
        return context == null || context.contextType() == null
                ? AiCallContextType.UNSPECIFIED
                : context.contextType();
    }

    private String buildPromptSummary(AiGenerationContext context, eu.catlabs.humanaity.ai.domain.AiPrompt prompt) {
        String prefix = context == null ? null : context.reasonSummary();
        String promptText = prompt == null ? null : firstNonBlank(prompt.getUserMessage(), prompt.getSystemMessage());
        String summary = firstNonBlank(prefix, promptText);
        if (summary == null) {
            return null;
        }
        String normalized = summary.replaceAll("\\s+", " ").trim();
        return trimToLength(normalized, PROMPT_SUMMARY_MAX_LENGTH);
    }

    private String buildResponseSummary(AiResponse response) {
        String responseText = response == null ? null : response.getRawContent();
        if (responseText == null || responseText.isBlank()) {
            return null;
        }
        String normalized = responseText.replaceAll("\\s+", " ").trim();
        return trimToLength(normalized, RESPONSE_SUMMARY_MAX_LENGTH);
    }

    private String hashPrompt(eu.catlabs.humanaity.ai.domain.AiPrompt prompt) {
        if (prompt == null) {
            return null;
        }
        String payload = nullToEmpty(prompt.getSystemMessage()) + "\n---\n" + nullToEmpty(prompt.getUserMessage());
        return hashText(payload);
    }

    private String hashText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required for AI log hashing", ex);
        }
    }

    private String resolveErrorCode(Exception exception) {
        if (exception == null) {
            return null;
        }
        if (exception instanceof AiServiceException aiServiceException && aiServiceException.getCause() != null) {
            return aiServiceException.getCause().getClass().getSimpleName();
        }
        return exception.getClass().getSimpleName();
    }

    private String resolveErrorMessage(Exception exception) {
        if (exception == null) {
            return null;
        }
        Throwable root = exception;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return firstNonBlank(root.getMessage(), exception.getMessage(), exception.getClass().getSimpleName());
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private List<AiCallLogGroupSummary> summarizeBreakdown(List<AiCallLog> logs, java.util.function.Function<AiCallLog, String> extractor) {
        return logs.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        log -> normalizeGroupKey(extractor.apply(log)),
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ))
                .entrySet()
                .stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> {
                    List<AiCallLog> groupedLogs = entry.getValue();
                    long successCount = groupedLogs.stream().filter(AiCallLog::isSuccess).count();
                    long fallbackCount = groupedLogs.stream().filter(AiCallLog::isFallbackUsed).count();
                    return new AiCallLogGroupSummary(
                            entry.getKey(),
                            groupedLogs.size(),
                            successCount,
                            groupedLogs.size() - successCount,
                            fallbackCount
                    );
                })
                .toList();
    }

    private String normalizeGroupKey(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }
        return value.trim();
    }

    public record AiCallLogSummary(
            long totalCount,
            long successCount,
            long failureCount,
            long fallbackCount,
            long averageDurationMs,
            Instant latestRequestedAt,
            List<AiCallLogContextSummary> byContextType,
            List<AiCallLogGroupSummary> byProvider,
            List<AiCallLogGroupSummary> byModel
    ) {
    }

    public record AiCallLogContextSummary(
            AiCallContextType contextType,
            long totalCount,
            long successCount,
            long failureCount,
            long fallbackCount
    ) {
    }

    public record AiCallLogGroupSummary(
            String key,
            long totalCount,
            long successCount,
            long failureCount,
            long fallbackCount
    ) {
    }
}
