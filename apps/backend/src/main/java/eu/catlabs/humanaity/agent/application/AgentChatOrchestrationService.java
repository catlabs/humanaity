package eu.catlabs.humanaity.agent.application;

import eu.catlabs.humanaity.agent.api.dto.AgentActionOutput;
import eu.catlabs.humanaity.agent.api.dto.AgentChatRequestInput;
import eu.catlabs.humanaity.agent.api.dto.AgentChatResponseOutput;
import eu.catlabs.humanaity.agent.api.dto.AgentReferencedEntitiesOutput;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class AgentChatOrchestrationService {

    private final CityRepository cityRepository;

    public AgentChatOrchestrationService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public AgentChatResponseOutput orchestrate(Long cityId, User currentUser, AgentChatRequestInput input) {
        validateInput(input);

        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + cityId));

        ensureOwnership(city, currentUser);

        String normalizedMessage = input.getMessage().trim().toLowerCase(Locale.ROOT);
        String intent = classifyIntent(normalizedMessage);

        AgentChatResponseOutput response = new AgentChatResponseOutput();
        response.setConversationId(resolveConversationId(input.getConversationId()));
        response.setCommandClass("SAFE_MVP");

        AgentReferencedEntitiesOutput referenced = new AgentReferencedEntitiesOutput();
        referenced.setCityId(cityId);
        response.setReferencedEntities(referenced);

        switch (intent) {
            case "step" -> {
                response.getExecutedActions().add(new AgentActionOutput(
                        "STEP_SIMULATION",
                        "PENDING",
                        "Recognized a bounded simulation step command"
                ));
                response.setMessage("Step command recognized. Safe execution wiring is enabled in the next task.");
            }
            case "snapshot" -> {
                response.getExecutedActions().add(new AgentActionOutput(
                        "READ_SNAPSHOT",
                        "PENDING",
                        "Recognized a latest snapshot request"
                ));
                response.setMessage("Snapshot command recognized. Safe execution wiring is enabled in the next task.");
            }
            case "summary" -> {
                response.getExecutedActions().add(new AgentActionOutput(
                        "READ_SUMMARY",
                        "PENDING",
                        "Recognized a recent city summary request"
                ));
                response.setMessage("Summary command recognized. Safe execution wiring is enabled in the next task.");
            }
            case "explain_event" -> {
                response.getExecutedActions().add(new AgentActionOutput(
                        "EXPLAIN_EVENT",
                        "PENDING",
                        "Recognized an event explanation request"
                ));
                response.setMessage("Event explanation command recognized. Safe execution wiring is enabled in the next task.");
            }
            case "recent_inventions" -> {
                response.getExecutedActions().add(new AgentActionOutput(
                        "READ_INVENTIONS",
                        "PENDING",
                        "Recognized a recent inventions request"
                ));
                response.setMessage("Recent inventions command recognized. Safe execution wiring is enabled in the next task.");
            }
            default -> {
                response.getExecutedActions().add(new AgentActionOutput(
                        "UNSUPPORTED_REQUEST",
                        "REJECTED",
                        "Request is outside the Sprint 8 safe command boundary"
                ));
                response.setMessage("I can currently handle safe Sprint 8 commands: step, snapshot, summary, explain event, and recent inventions.");
            }
        }

        return response;
    }

    private void validateInput(AgentChatRequestInput input) {
        if (input == null || input.getMessage() == null || input.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("message is required");
        }
    }

    private void ensureOwnership(City city, User currentUser) {
        Objects.requireNonNull(currentUser, "currentUser must not be null");
        if (city.getOwner() == null || city.getOwner().getId() == null || currentUser.getId() == null) {
            throw new AccessDeniedException("City ownership cannot be verified");
        }
        if (!city.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not own this city");
        }
    }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return conversationId;
    }

    private String classifyIntent(String normalizedMessage) {
        if (containsAny(normalizedMessage, "step", "advance", "ticks")) {
            return "step";
        }
        if (containsAny(normalizedMessage, "snapshot", "state", "latest")) {
            return "snapshot";
        }
        if (containsAny(normalizedMessage, "summary", "summarize", "happening")) {
            return "summary";
        }
        if (containsAny(normalizedMessage, "explain", "event")) {
            return "explain_event";
        }
        if (containsAny(normalizedMessage, "invention", "inventions")) {
            return "recent_inventions";
        }
        return "unknown";
    }

    private boolean containsAny(String message, String... patterns) {
        for (String pattern : patterns) {
            if (message.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
