package eu.catlabs.humanaity.ai.api.dto;

import java.util.List;

public record AiCallLogSummaryOutput(
        long totalCount,
        long successCount,
        long failureCount,
        long fallbackCount,
        List<AiCallLogContextSummaryOutput> byContextType
) {
}
