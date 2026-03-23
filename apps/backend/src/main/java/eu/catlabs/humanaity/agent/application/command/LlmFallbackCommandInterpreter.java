package eu.catlabs.humanaity.agent.application.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.ai.application.AiGenerationService;
import eu.catlabs.humanaity.ai.application.AiGenerationContext;
import eu.catlabs.humanaity.ai.domain.AiCallContextType;
import eu.catlabs.humanaity.ai.domain.AiPrompt;
import eu.catlabs.humanaity.ai.domain.AiResponse;
import eu.catlabs.humanaity.ai.infrastructure.port.AiServiceException;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.simulation.application.SimulationPlaceRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LlmFallbackCommandInterpreter {

    private final AiGenerationService aiGenerationService;
    private final ObjectMapper objectMapper;

    public LlmFallbackCommandInterpreter(AiGenerationService aiGenerationService, ObjectMapper objectMapper) {
        this.aiGenerationService = aiGenerationService;
        this.objectMapper = objectMapper;
    }

    public FallbackCommandMatch interpret(String normalizedMessage, List<Human> humans) {
        if (!isAllowedFallbackCandidate(normalizedMessage)) {
            return FallbackCommandMatch.notApplicable();
        }

        try {
            AiPrompt prompt = AiPrompt.builder()
                    .systemMessage("""
                            You interpret HUMANAIty chat requests into a tiny JSON command.
                            Return only JSON.
                            Allowed command types: MOVE_TO_PLACE, MEET_HUMAN, UNSUPPORTED.
                            Use the provided human names exactly as listed.
                            Use the provided place ids exactly as listed.
                            """)
                    .userMessage(buildUserPrompt(normalizedMessage, humans))
                    .responseFormat(AiPrompt.ResponseFormat.JSON_OBJECT)
                    .build();

            AiResponse response = aiGenerationService.generate(prompt, new AiGenerationContext(
                    AiCallContextType.CHAT_FALLBACK,
                    humans.stream().findFirst().map(human -> human.getCity() == null ? null : human.getCity().getId()).orElse(null),
                    "AGENT_CHAT",
                    normalizedMessage,
                    "Interpret an ambiguous agent chat command into a deterministic command.",
                    true
            ));
            JsonNode json = response.getJsonContent(objectMapper);
            if (json == null || !json.isObject()) {
                aiGenerationService.markFallbackUsed(response.getLogId());
                return FallbackCommandMatch.invalid("Fallback response was not valid JSON");
            }
            FallbackCommandMatch match = validate(json, humans);
            if (match.status() != FallbackCommandMatchStatus.MATCHED) {
                aiGenerationService.markFallbackUsed(response.getLogId());
            }
            return match;
        } catch (AiServiceException ex) {
            return FallbackCommandMatch.invalid("LLM fallback failed");
        }
    }

    private boolean isAllowedFallbackCandidate(String message) {
        return containsAny(message,
                "gathering",
                "gather",
                "interact",
                "social",
                "where people are",
                "where people are gathering");
    }

    private boolean containsAny(String message, String... patterns) {
        for (String pattern : patterns) {
            if (message.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private String buildUserPrompt(String normalizedMessage, List<Human> humans) {
        String humanNames = humans.stream()
                .map(Human::getName)
                .filter(name -> name != null && !name.isBlank())
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.joining(", "));
        String placeIds = SimulationPlaceRegistry.all().stream()
                .map(SimulationPlaceRegistry.SimulationPlace::id)
                .collect(Collectors.joining(", "));

        return """
                Message:
                %s

                Humans:
                %s

                Places:
                %s

                Return JSON with:
                {
                  "type": "MOVE_TO_PLACE" | "MEET_HUMAN" | "UNSUPPORTED",
                  "primaryHumanName": "string or null",
                  "secondaryHumanName": "string or null",
                  "placeId": "string or null"
                }
                """.formatted(normalizedMessage, humanNames, placeIds);
    }

    private FallbackCommandMatch validate(JsonNode json, List<Human> humans) {
        String type = textOrNull(json.get("type"));
        if (type == null || type.isBlank() || "UNSUPPORTED".equalsIgnoreCase(type)) {
            return FallbackCommandMatch.invalid("Fallback did not return a supported command");
        }

        return switch (type.toUpperCase(Locale.ROOT)) {
            case "MOVE_TO_PLACE" -> validateMoveToPlace(json, humans);
            case "MEET_HUMAN" -> validateMeetHuman(json, humans);
            default -> FallbackCommandMatch.invalid("Fallback returned unsupported command type");
        };
    }

    private FallbackCommandMatch validateMoveToPlace(JsonNode json, List<Human> humans) {
        Human human = resolveHumanByName(humans, textOrNull(json.get("primaryHumanName")));
        String placeId = textOrNull(json.get("placeId"));
        boolean knownPlace = SimulationPlaceRegistry.all().stream().anyMatch(place -> place.id().equals(placeId));
        if (human == null || !knownPlace) {
            return FallbackCommandMatch.invalid("Fallback move command could not be validated");
        }
        return FallbackCommandMatch.matched(AgentChatCommand.moveToPlace(human.getId(), placeId));
    }

    private FallbackCommandMatch validateMeetHuman(JsonNode json, List<Human> humans) {
        Human left = resolveHumanByName(humans, textOrNull(json.get("primaryHumanName")));
        Human right = resolveHumanByName(humans, textOrNull(json.get("secondaryHumanName")));
        if (left == null || right == null || left.getId().equals(right.getId())) {
            return FallbackCommandMatch.invalid("Fallback meet command could not be validated");
        }
        return FallbackCommandMatch.matched(AgentChatCommand.meetHuman(left.getId(), right.getId()));
    }

    private Human resolveHumanByName(List<Human> humans, String humanName) {
        if (humanName == null || humanName.isBlank()) {
            return null;
        }
        List<Human> matches = humans.stream()
                .filter(h -> h.getName() != null)
                .filter(h -> h.getName().equalsIgnoreCase(humanName))
                .toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }
}
