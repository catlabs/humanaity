package eu.catlabs.humanaity.ai.application.prompt;

import eu.catlabs.humanaity.ai.domain.AiPrompt;
import eu.catlabs.humanaity.invention.domain.Invention;
import org.springframework.stereotype.Component;

@Component
public class InventionEnrichmentPrompt {

    public AiPrompt createPrompt(Invention invention) {
        String systemMessage = "You enrich deterministic invention facts. Return strict JSON only.";
        String userMessage = String.format("""
                Return one JSON object with this exact shape:
                {
                  "title": "string",
                  "summary": "string"
                }

                Deterministic invention facts:
                - inventionKey: %s
                - category: %s
                - canonicalTitle: %s
                - canonicalSummary: %s
                - yearCreated: %d
                - eraCreated: %s
                - impactScore: %d

                Rules:
                - Keep title <= 120 chars.
                - Keep summary <= 400 chars.
                - Do not invent deterministic IDs, dates, or actors.
                - Preserve factual meaning, improve readability.
                - Return strict JSON only, no markdown and no extra text.
                """,
                invention.getInventionKey(),
                invention.getCategory(),
                invention.getTitle(),
                invention.getSummary(),
                invention.getYearCreated(),
                invention.getEraCreated(),
                invention.getImpactScore()
        );

        return AiPrompt.builder()
                .systemMessage(systemMessage)
                .userMessage(userMessage)
                .responseFormat(AiPrompt.ResponseFormat.JSON_OBJECT)
                .build();
    }
}
