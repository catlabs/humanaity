package eu.catlabs.humanaity.ai.api.dto;

import java.time.Instant;

public record AiCallLogOutput(
        Long id,
        Instant timestamp,
        Long cityId,
        String contextType,
        String contextEntityType,
        String contextEntityId,
        String provider,
        String model,
        boolean success,
        boolean fallbackUsed,
        long durationMs,
        String promptSummary,
        String promptHash,
        String responseHash,
        String errorCode,
        String errorMessage
) {
}
