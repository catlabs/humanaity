package eu.catlabs.humanaity.simulation.application.tribe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.ai.application.AiGenerationService;
import eu.catlabs.humanaity.ai.application.AiGenerationContext;
import eu.catlabs.humanaity.ai.domain.AiCallContextType;
import eu.catlabs.humanaity.ai.domain.AiPrompt;
import eu.catlabs.humanaity.ai.domain.AiResponse;
import eu.catlabs.humanaity.ai.infrastructure.port.AiServiceException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class ValidatedLlmTribeDecisionSelector {

    private final AiGenerationService aiGenerationService;
    private final ObjectMapper objectMapper;

    public ValidatedLlmTribeDecisionSelector(AiGenerationService aiGenerationService, ObjectMapper objectMapper) {
        this.aiGenerationService = aiGenerationService;
        this.objectMapper = objectMapper;
    }

    public Optional<TribeDecisionCandidate> select(List<TribeDecisionCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        List<TribeDecisionCandidate> ordered = candidates.stream()
                .sorted(Comparator.comparingInt(TribeDecisionCandidate::priority).reversed()
                        .thenComparing(TribeDecisionCandidate::candidateId))
                .toList();

        try {
            AiPrompt prompt = AiPrompt.builder()
                    .systemMessage("""
                            You choose one tribe decision candidate.
                            Return only JSON.
                            Allowed output shape: {"candidateId":"string"}
                            Choose only from the candidate ids provided by the user.
                            """)
                    .userMessage(buildUserPrompt(ordered))
                    .responseFormat(AiPrompt.ResponseFormat.JSON_OBJECT)
                    .build();

            AiResponse response = aiGenerationService.generate(prompt, new AiGenerationContext(
                    AiCallContextType.CHIEF_DECISION,
                    ordered.stream().findFirst().map(ignored -> (Long) null).orElse(null),
                    "TRIBE_DECISION",
                    ordered.stream().map(TribeDecisionCandidate::tribeId).distinct().collect(Collectors.joining(",")),
                    "Select a tribe decision candidate from deterministic candidates.",
                    true
            ));
            JsonNode json = response.getJsonContent(objectMapper);
            if (json == null || !json.isObject()) {
                aiGenerationService.markFallbackUsed(response.getLogId());
                return deterministicFallback(ordered);
            }

            String candidateId = textOrNull(json.get("candidateId"));
            if (candidateId == null || candidateId.isBlank()) {
                aiGenerationService.markFallbackUsed(response.getLogId());
                return deterministicFallback(ordered);
            }

            Optional<TribeDecisionCandidate> selected = ordered.stream()
                    .filter(candidate -> candidate.candidateId().equals(candidateId))
                    .findFirst()
                    .or(() -> deterministicFallback(ordered));
            if (selected.isEmpty() || !selected.get().candidateId().equals(candidateId)) {
                aiGenerationService.markFallbackUsed(response.getLogId());
            }
            return selected;
        } catch (AiServiceException ex) {
            return deterministicFallback(ordered);
        }
    }

    private String buildUserPrompt(List<TribeDecisionCandidate> candidates) {
        return candidates.stream()
                .map(candidate -> {
                    String members = candidate.memberIds() == null ? "" : candidate.memberIds().stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","));
                    return "%s | %s | tribe=%s | human=%s | members=%s | place=%s | priority=%d"
                            .formatted(candidate.candidateId(), candidate.description(), candidate.tribeId(), candidate.humanId(), members, candidate.placeId(), candidate.priority());
                })
                .collect(Collectors.joining("\n"));
    }

    private Optional<TribeDecisionCandidate> deterministicFallback(List<TribeDecisionCandidate> ordered) {
        return ordered.stream().findFirst();
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }
}
