package eu.catlabs.humanaity.ai.application;

import eu.catlabs.humanaity.ai.domain.AiCallContextType;

public record AiGenerationContext(
        AiCallContextType contextType,
        Long cityId,
        String contextEntityType,
        String contextEntityId,
        String reasonSummary,
        boolean fallbackExpected
) {

    public static AiGenerationContext unspecified() {
        return new AiGenerationContext(AiCallContextType.UNSPECIFIED, null, null, null, null, false);
    }
}
