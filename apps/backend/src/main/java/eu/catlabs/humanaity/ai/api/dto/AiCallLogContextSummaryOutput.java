package eu.catlabs.humanaity.ai.api.dto;

public record AiCallLogContextSummaryOutput(
        String contextType,
        long totalCount,
        long successCount,
        long failureCount,
        long fallbackCount
) {
}
