package eu.catlabs.humanaity.ai.application.prompt;

import eu.catlabs.humanaity.ai.domain.AiPrompt;
import eu.catlabs.humanaity.event.domain.Event;
import org.springframework.stereotype.Component;

@Component
public class EventDialogueEnrichmentPrompt {

    public AiPrompt createPrompt(Event event) {
        String systemMessage = "You enrich deterministic dialogue event facts. Return strict JSON only.";
        String userMessage = String.format("""
                Return one JSON object with this exact shape:
                {
                  "snippet": "string"
                }

                Deterministic event facts:
                - eventKey: %s
                - eventType: %s
                - eventCategory: %s
                - year: %d
                - era: %s
                - actorIds: %s
                - payload: %s

                Rules:
                - Keep snippet <= 300 chars.
                - The snippet must stay consistent with deterministic facts.
                - Do not invent actor IDs, years, eras, or categories.
                - Return strict JSON only, no markdown and no extra text.
                """,
                event.getEventKey(),
                event.getEventType(),
                event.getEventCategory(),
                event.getYear(),
                event.getEra(),
                event.getActorIds(),
                event.getPayload()
        );

        return AiPrompt.builder()
                .systemMessage(systemMessage)
                .userMessage(userMessage)
                .responseFormat(AiPrompt.ResponseFormat.JSON_OBJECT)
                .build();
    }
}
