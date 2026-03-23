package eu.catlabs.humanaity.ai.application.enrichment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.ai.application.AiGenerationService;
import eu.catlabs.humanaity.ai.application.AiGenerationContext;
import eu.catlabs.humanaity.ai.application.prompt.EventDialogueEnrichmentPrompt;
import eu.catlabs.humanaity.ai.application.prompt.InventionEnrichmentPrompt;
import eu.catlabs.humanaity.ai.domain.AiCallContextType;
import eu.catlabs.humanaity.ai.domain.AiEnrichmentStatus;
import eu.catlabs.humanaity.ai.domain.AiResponse;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.domain.EventType;
import eu.catlabs.humanaity.invention.domain.Invention;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AiHistoryEnrichmentService {

    private final AiGenerationService aiGenerationService;
    private final InventionEnrichmentPrompt inventionEnrichmentPrompt;
    private final EventDialogueEnrichmentPrompt eventDialogueEnrichmentPrompt;
    private final ObjectMapper objectMapper;

    public AiHistoryEnrichmentService(
            AiGenerationService aiGenerationService,
            InventionEnrichmentPrompt inventionEnrichmentPrompt,
            EventDialogueEnrichmentPrompt eventDialogueEnrichmentPrompt,
            ObjectMapper objectMapper
    ) {
        this.aiGenerationService = aiGenerationService;
        this.inventionEnrichmentPrompt = inventionEnrichmentPrompt;
        this.eventDialogueEnrichmentPrompt = eventDialogueEnrichmentPrompt;
        this.objectMapper = objectMapper;
    }

    public void enrichInvention(Invention invention) {
        try {
            AiResponse response = aiGenerationService.generate(
                    inventionEnrichmentPrompt.createPrompt(invention),
                    new AiGenerationContext(
                            AiCallContextType.INVENTION_ENRICHMENT,
                            invention.getCity() == null ? null : invention.getCity().getId(),
                            "INVENTION",
                            invention.getId() == null ? invention.getInventionKey() : String.valueOf(invention.getId()),
                            "Enrich a persisted invention summary and title.",
                            true
                    )
            );
            JsonNode json = response.getJsonContent(objectMapper);

            String enrichedTitle = trimToNull(json == null ? null : json.path("title").asText(null));
            String enrichedSummary = trimToNull(json == null ? null : json.path("summary").asText(null));

            if (enrichedTitle == null || enrichedSummary == null) {
                aiGenerationService.markFallbackUsed(response.getLogId());
                applyInventionFallback(invention);
                return;
            }

            invention.setEnrichedTitle(limit(enrichedTitle, 200));
            invention.setEnrichedSummary(limit(enrichedSummary, 2000));
            invention.setEnrichmentStatus(AiEnrichmentStatus.READY);
            invention.setEnrichmentFallback(false);
            invention.setEnrichmentProvider(response.getProvider() == null ? null : response.getProvider().name());
            invention.setEnrichmentModel(response.getModel() == null ? "OPENAI_CHAT" : response.getModel());
            invention.setEnrichmentUpdatedAt(Instant.now());
        } catch (Exception ex) {
            applyInventionFallback(invention);
        }
    }

    public void enrichEventDialogueIfEligible(Event event) {
        if (event.getEventType() != EventType.DIALOGUE_EXCHANGED) {
            event.setEnrichmentStatus(AiEnrichmentStatus.NONE);
            event.setEnrichmentFallback(false);
            event.setEnrichedSnippet(null);
            event.setEnrichmentProvider(null);
            event.setEnrichmentModel(null);
            event.setEnrichmentUpdatedAt(null);
            return;
        }

        try {
            AiResponse response = aiGenerationService.generate(
                    eventDialogueEnrichmentPrompt.createPrompt(event),
                    new AiGenerationContext(
                            AiCallContextType.EVENT_ENRICHMENT,
                            event.getCity() == null ? null : event.getCity().getId(),
                            "EVENT",
                            event.getId() == null ? event.getEventKey() : String.valueOf(event.getId()),
                            "Enrich a dialogue event snippet for history presentation.",
                            true
                    )
            );
            JsonNode json = response.getJsonContent(objectMapper);
            String snippet = trimToNull(json == null ? null : json.path("snippet").asText(null));

            if (snippet == null) {
                aiGenerationService.markFallbackUsed(response.getLogId());
                applyEventFallback(event);
                return;
            }

            event.setEnrichedSnippet(limit(snippet, 2000));
            event.setEnrichmentStatus(AiEnrichmentStatus.READY);
            event.setEnrichmentFallback(false);
            event.setEnrichmentProvider(response.getProvider() == null ? null : response.getProvider().name());
            event.setEnrichmentModel(response.getModel() == null ? "OPENAI_CHAT" : response.getModel());
            event.setEnrichmentUpdatedAt(Instant.now());
        } catch (Exception ex) {
            applyEventFallback(event);
        }
    }

    private void applyInventionFallback(Invention invention) {
        invention.setEnrichedTitle(limit(invention.getTitle(), 200));
        invention.setEnrichedSummary(limit("Fallback: " + invention.getSummary(), 2000));
        invention.setEnrichmentStatus(AiEnrichmentStatus.FALLBACK);
        invention.setEnrichmentFallback(true);
        invention.setEnrichmentProvider("FALLBACK");
        invention.setEnrichmentModel("NONE");
        invention.setEnrichmentUpdatedAt(Instant.now());
    }

    private void applyEventFallback(Event event) {
        String snippet = "Fallback: " + event.getEventType().name() + " at tick " + event.getTick();
        event.setEnrichedSnippet(limit(snippet, 2000));
        event.setEnrichmentStatus(AiEnrichmentStatus.FALLBACK);
        event.setEnrichmentFallback(true);
        event.setEnrichmentProvider("FALLBACK");
        event.setEnrichmentModel("NONE");
        event.setEnrichmentUpdatedAt(Instant.now());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
