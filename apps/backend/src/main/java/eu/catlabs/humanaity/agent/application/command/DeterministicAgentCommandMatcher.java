package eu.catlabs.humanaity.agent.application.command;

import eu.catlabs.humanaity.agent.api.dto.AgentChatRequestInput;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.simulation.application.SimulationPlaceRegistry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DeterministicAgentCommandMatcher {

    private static final int MAX_SAFE_STEPS = 50;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");
    private static final Map<String, List<String>> PLACE_KEYWORDS = createPlaceKeywords();

    private final HumanRepository humanRepository;

    public DeterministicAgentCommandMatcher(HumanRepository humanRepository) {
        this.humanRepository = humanRepository;
    }

    public DeterministicCommandMatch match(Long cityId, AgentChatRequestInput input) {
        String normalizedMessage = normalize(input.getMessage());
        if (normalizedMessage.isBlank()) {
            return DeterministicCommandMatch.unsupported("Message is blank");
        }
        if (Boolean.TRUE.equals(input.getConfirmIntervention()) && input.getConfirmationToken() != null) {
            return DeterministicCommandMatch.unsupported("Confirmed intervention requests stay outside deterministic safe matching");
        }
        if (normalizedMessage.contains("director")) {
            return DeterministicCommandMatch.unsupported("Director commands stay outside deterministic safe matching");
        }

        List<Human> humans = humanRepository.findByCityIdOrderByIdAsc(cityId);

        if (isPauseMessage(normalizedMessage)) {
            return DeterministicCommandMatch.matched(AgentChatCommand.pause());
        }

        if (isStepMessage(normalizedMessage)) {
            return DeterministicCommandMatch.matched(AgentChatCommand.step(extractRequestedStepCount(normalizedMessage)));
        }

        if (isMoveToPlaceMessage(normalizedMessage)) {
            String placeId = resolvePlaceId(normalizedMessage);
            if (placeId == null) {
                return DeterministicCommandMatch.ambiguous("No supported place could be resolved");
            }
            Human target = resolveSingleHuman(humans, input, normalizedMessage);
            if (target == null) {
                return DeterministicCommandMatch.ambiguous("No single human could be resolved for move command");
            }
            return DeterministicCommandMatch.matched(AgentChatCommand.moveToPlace(target.getId(), placeId));
        }

        if (isFocusMessage(normalizedMessage)) {
            Human target = resolveSingleHuman(humans, input, normalizedMessage);
            if (target == null) {
                return DeterministicCommandMatch.ambiguous("No single human could be resolved for focus command");
            }
            return DeterministicCommandMatch.matched(AgentChatCommand.focusHuman(target.getId()));
        }

        if (isMeetMessage(normalizedMessage)) {
            List<Human> pair = resolveHumanPair(humans, input, normalizedMessage);
            if (pair == null) {
                return DeterministicCommandMatch.ambiguous("Two distinct humans are required for meet command");
            }
            return DeterministicCommandMatch.matched(AgentChatCommand.meetHuman(pair.get(0).getId(), pair.get(1).getId()));
        }

        return DeterministicCommandMatch.unsupported("No deterministic command family matched");
    }

    private String normalize(String message) {
        return message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isStepMessage(String message) {
        return containsAny(message, "step", "advance");
    }

    private boolean isPauseMessage(String message) {
        return containsAny(message, "pause simulation", "pause", "stop simulation", "halt simulation");
    }

    private boolean isFocusMessage(String message) {
        return containsAny(message, "focus", "inspect");
    }

    private boolean isMoveToPlaceMessage(String message) {
        return containsAny(message, "go to", "move", "send", "travel") && resolvePlaceId(message) != null;
    }

    private boolean isMeetMessage(String message) {
        return containsAny(message, "meet", "introduce");
    }

    private boolean containsAny(String message, String... patterns) {
        for (String pattern : patterns) {
            if (message.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private int extractRequestedStepCount(String message) {
        return extractFirstNumber(message)
                .map(number -> Math.toIntExact(Math.min(number, MAX_SAFE_STEPS)))
                .orElse(1);
    }

    private Optional<Long> extractFirstNumber(String message) {
        Matcher matcher = NUMBER_PATTERN.matcher(message);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(matcher.group(1)));
    }

    private List<Long> extractNumbers(String message) {
        Matcher matcher = NUMBER_PATTERN.matcher(message);
        List<Long> numbers = new ArrayList<>();
        while (matcher.find()) {
            numbers.add(Long.parseLong(matcher.group(1)));
        }
        return numbers;
    }

    private String resolvePlaceId(String normalizedMessage) {
        for (SimulationPlaceRegistry.SimulationPlace place : SimulationPlaceRegistry.all()) {
            List<String> aliases = PLACE_KEYWORDS.getOrDefault(place.id(), List.of(place.id()));
            if (aliases.stream().anyMatch(normalizedMessage::contains)) {
                return place.id();
            }
        }
        return null;
    }

    private Human resolveSingleHuman(List<Human> humans, AgentChatRequestInput input, String normalizedMessage) {
        if (humans.isEmpty()) {
            return null;
        }
        if (input.getSelectedHumanId() != null) {
            Human preferred = findById(humans, input.getSelectedHumanId());
            if (preferred != null) {
                return preferred;
            }
        }

        List<Human> byName = humans.stream()
                .filter(h -> h.getName() != null && normalizedMessage.contains(h.getName().toLowerCase(Locale.ROOT)))
                .toList();
        if (byName.size() == 1) {
            return byName.get(0);
        }
        if (byName.size() > 1) {
            return null;
        }

        List<Long> numbers = extractNumbers(normalizedMessage);
        if (numbers.size() == 1) {
            return findById(humans, numbers.get(0));
        }
        return null;
    }

    private List<Human> resolveHumanPair(List<Human> humans, AgentChatRequestInput input, String normalizedMessage) {
        if (humans.size() < 2) {
            return null;
        }

        List<Human> resolved = new ArrayList<>();
        if (input.getSelectedHumanId() != null) {
            Human preferred = findById(humans, input.getSelectedHumanId());
            if (preferred != null) {
                resolved.add(preferred);
            }
        }

        humans.stream()
                .filter(h -> h.getName() != null && normalizedMessage.contains(h.getName().toLowerCase(Locale.ROOT)))
                .forEach(h -> addUnique(resolved, h));

        if (resolved.size() < 2) {
            for (Long parsedId : extractNumbers(normalizedMessage)) {
                Human byId = findById(humans, parsedId);
                if (byId != null) {
                    addUnique(resolved, byId);
                }
            }
        }

        if (resolved.size() < 2) {
            return null;
        }
        return List.of(resolved.get(0), resolved.get(1));
    }

    private Human findById(List<Human> humans, Long id) {
        return humans.stream()
                .filter(h -> h.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private void addUnique(List<Human> humans, Human candidate) {
        boolean exists = humans.stream().anyMatch(h -> h.getId().equals(candidate.getId()));
        if (!exists) {
            humans.add(candidate);
        }
    }

    private static Map<String, List<String>> createPlaceKeywords() {
        Map<String, List<String>> keywords = new LinkedHashMap<>();
        keywords.put("forest", List.of("forest", "woods"));
        keywords.put("river", List.of("river", "water"));
        keywords.put("church", List.of("church", "temple"));
        keywords.put("campfire", List.of("campfire", "fire"));
        keywords.put("house", List.of("house", "home"));
        return keywords;
    }
}
