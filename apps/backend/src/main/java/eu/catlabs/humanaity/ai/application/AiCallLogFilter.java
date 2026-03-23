package eu.catlabs.humanaity.ai.application;

import eu.catlabs.humanaity.ai.domain.AiCallContextType;

public record AiCallLogFilter(
        Long cityId,
        AiCallContextType contextType,
        Boolean success,
        Boolean fallbackUsed,
        String provider,
        String model,
        Integer limit
) {
}
