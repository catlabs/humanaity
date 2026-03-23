package eu.catlabs.humanaity.simulation.application.tribe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.ai.application.AiGenerationContext;
import eu.catlabs.humanaity.ai.application.AiGenerationService;
import eu.catlabs.humanaity.ai.domain.AiCallContextType;
import eu.catlabs.humanaity.ai.domain.AiPrompt;
import eu.catlabs.humanaity.ai.domain.AiResponse;
import eu.catlabs.humanaity.ai.infrastructure.port.AiServiceException;
import eu.catlabs.humanaity.simulation.domain.TribeDecisionSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TribeChiefDecisionResolver {

    private final AiGenerationService aiGenerationService;
    private final ObjectMapper objectMapper;
    private final boolean llmEnabled;

    public TribeChiefDecisionResolver(
            AiGenerationService aiGenerationService,
            ObjectMapper objectMapper,
            @Value("${humanaity.simulation.tribe-chief.llm-enabled:false}") boolean llmEnabled
    ) {
        this.aiGenerationService = aiGenerationService;
        this.objectMapper = objectMapper;
        this.llmEnabled = llmEnabled;
    }

    public TribeDecisionResolution resolve(Long cityId, List<TribeDecisionCandidate> candidates) {
        List<TribeDecisionCandidate> ordered = orderCandidates(candidates);
        if (ordered.isEmpty()) {
            return new TribeDecisionResolution(null, TribeDecisionSource.DETERMINISTIC, "no-options");
        }

        TribeDecisionCandidate deterministic = selectDeterministic(ordered);
        if (!llmEnabled) {
            return new TribeDecisionResolution(deterministic, TribeDecisionSource.DETERMINISTIC, null);
        }

        try {
            AiResponse response = aiGenerationService.generate(
                    AiPrompt.builder()
                            .systemMessage("""
                                    You choose exactly one tribe chief option.
                                    Return only JSON in the shape {"selectedOptionId":"string"}.
                                    Choose only from the option ids provided by the user.
                                    """)
                            .userMessage(buildUserMessage(ordered))
                            .responseFormat(AiPrompt.ResponseFormat.JSON_OBJECT)
                            .build(),
                    new AiGenerationContext(
                            AiCallContextType.CHIEF_DECISION,
                            cityId,
                            "TRIBE_DECISION",
                            ordered.stream().map(TribeDecisionCandidate::tribeId).distinct().collect(Collectors.joining(",")),
                            "Select a tribe decision candidate from deterministic candidates.",
                            true
                    )
            );

            JsonNode json = response.getJsonContent(objectMapper);
            if (json == null || !json.isObject()) {
                return new TribeDecisionResolution(deterministic, TribeDecisionSource.DETERMINISTIC_FALLBACK, "invalid-json");
            }

            String selectedOptionId = textOrNull(json.get("selectedOptionId"));
            if (selectedOptionId == null || selectedOptionId.isBlank()) {
                return new TribeDecisionResolution(deterministic, TribeDecisionSource.DETERMINISTIC_FALLBACK, "missing-selectedOptionId");
            }

            Optional<TribeDecisionCandidate> selected = ordered.stream()
                    .filter(candidate -> candidate.candidateId().equals(selectedOptionId))
                    .findFirst();
            if (selected.isEmpty()) {
                return new TribeDecisionResolution(deterministic, TribeDecisionSource.DETERMINISTIC_FALLBACK, "unknown-option-id");
            }

            return new TribeDecisionResolution(selected.get(), TribeDecisionSource.LLM, null);
        } catch (AiServiceException ex) {
            return new TribeDecisionResolution(deterministic, TribeDecisionSource.DETERMINISTIC_FALLBACK, "ai-unavailable");
        } catch (RuntimeException ex) {
            return new TribeDecisionResolution(deterministic, TribeDecisionSource.DETERMINISTIC_FALLBACK, "invalid-llm-response");
        }
    }

    private TribeDecisionCandidate selectDeterministic(List<TribeDecisionCandidate> ordered) {
        return ordered.stream()
                .filter(this::isActionable)
                .findFirst()
                .orElse(ordered.get(0));
    }

    private List<TribeDecisionCandidate> orderCandidates(List<TribeDecisionCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        return candidates.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((TribeDecisionCandidate candidate) -> isActionable(candidate) ? 0 : 1)
                        .thenComparing(candidate -> safe(candidate.placeId()))
                        .thenComparing(TribeDecisionCandidate::candidateId))
                .toList();
    }

    private boolean isActionable(TribeDecisionCandidate candidate) {
        return candidate.type() != TribeDecisionType.WAIT;
    }

    private String buildUserMessage(List<TribeDecisionCandidate> ordered) {
        return ordered.stream()
                .map(candidate -> {
                    String members = candidate.memberIds() == null || candidate.memberIds().isEmpty()
                            ? ""
                            : candidate.memberIds().stream().map(String::valueOf).collect(Collectors.joining(","));
                    return """
                            optionId=%s
                            type=%s
                            tribeId=%s
                            chiefHumanId=%s
                            placeId=%s
                            memberIds=%s
                            description=%s
                            """.formatted(
                            candidate.candidateId(),
                            candidate.type().name(),
                            safe(candidate.tribeId()),
                            candidate.humanId() == null ? "" : String.valueOf(candidate.humanId()),
                            safe(candidate.placeId()),
                            members,
                            safe(candidate.description())
                    ).trim();
                })
                .collect(Collectors.joining("\n\n"));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    public record TribeDecisionResolution(
            TribeDecisionCandidate candidate,
            TribeDecisionSource decisionSource,
            String fallbackReason
    ) {
    }
}
