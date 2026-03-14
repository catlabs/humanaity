package eu.catlabs.humanaity.agent.application;

import eu.catlabs.humanaity.agent.api.dto.AgentActionOutput;
import eu.catlabs.humanaity.agent.api.dto.AgentChatRequestInput;
import eu.catlabs.humanaity.agent.api.dto.AgentChatResponseOutput;
import eu.catlabs.humanaity.agent.api.dto.AgentReferencedEntitiesOutput;
import eu.catlabs.humanaity.agent.api.dto.AgentUiEffectOutput;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.invention.domain.Invention;
import eu.catlabs.humanaity.invention.infrastructure.persistence.InventionRepository;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.simulation.application.SimulationApplicationService;
import eu.catlabs.humanaity.simulation.application.query.SimulationReadModelQueryService;
import eu.catlabs.humanaity.simulation.domain.SimulationRun;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentChatOrchestrationService {

    private static final int MAX_SAFE_STEPS = 50;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");
    private static final Pattern FOLLOW_TICKS_PATTERN = Pattern.compile("\\bfor\\s+(\\d+)\\b");

    private final CityRepository cityRepository;
    private final SimulationApplicationService simulationApplicationService;
    private final SimulationReadModelQueryService simulationReadModelQueryService;
    private final EventRepository eventRepository;
    private final InventionRepository inventionRepository;
    private final HumanRepository humanRepository;

    public AgentChatOrchestrationService(
            CityRepository cityRepository,
            SimulationApplicationService simulationApplicationService,
            SimulationReadModelQueryService simulationReadModelQueryService,
            EventRepository eventRepository,
            InventionRepository inventionRepository,
            HumanRepository humanRepository
    ) {
        this.cityRepository = cityRepository;
        this.simulationApplicationService = simulationApplicationService;
        this.simulationReadModelQueryService = simulationReadModelQueryService;
        this.eventRepository = eventRepository;
        this.inventionRepository = inventionRepository;
        this.humanRepository = humanRepository;
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
            case "step" -> executeStep(cityId, normalizedMessage, response);
            case "snapshot" -> executeSnapshot(cityId, response);
            case "summary" -> executeSummary(cityId, response);
            case "explain_event" -> executeExplainEvent(cityId, input, normalizedMessage, response);
            case "recent_inventions" -> executeRecentInventions(cityId, response);
            case "focus_human" -> {
                response.setCommandClass("GUIDED");
                executeFocusHuman(cityId, input, normalizedMessage, response);
            }
            case "compare_humans" -> {
                response.setCommandClass("GUIDED");
                executeCompareHumans(cityId, input, normalizedMessage, response);
            }
            case "follow_human" -> {
                response.setCommandClass("GUIDED");
                executeFollowHuman(cityId, input, normalizedMessage, response);
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

    private void executeStep(Long cityId, String normalizedMessage, AgentChatResponseOutput response) {
        int requestedCount = extractRequestedStepCount(normalizedMessage);
        int count = Math.max(1, Math.min(requestedCount, MAX_SAFE_STEPS));

        SimulationRun run = simulationApplicationService.step(cityId, count);
        long fromTick = Math.max(0L, run.getTick() - count + 1);

        response.getExecutedActions().add(new AgentActionOutput(
                "STEP_SIMULATION",
                "COMPLETED",
                "Advanced simulation by " + count + " deterministic steps"
        ));

        AgentUiEffectOutput refreshSnapshot = new AgentUiEffectOutput("REFRESH_SNAPSHOT");
        AgentUiEffectOutput refreshTimeline = new AgentUiEffectOutput("REFRESH_TIMELINE");
        refreshTimeline.setFromTick(fromTick);
        response.getUiEffects().add(refreshSnapshot);
        response.getUiEffects().add(refreshTimeline);

        response.setMessage("Advanced the city by " + count + " step(s). Current tick is " + run.getTick() + ".");
    }

    private void executeSnapshot(Long cityId, AgentChatResponseOutput response) {
        SimulationReadModelQueryService.SimulationSnapshotProjection snapshot =
                simulationReadModelQueryService.getCitySnapshot(cityId, simulationApplicationService::isRunning);

        response.getExecutedActions().add(new AgentActionOutput(
                "READ_SNAPSHOT",
                "COMPLETED",
                "Loaded latest backend-owned city snapshot"
        ));
        response.getUiEffects().add(new AgentUiEffectOutput("REFRESH_SNAPSHOT"));

        List<Long> topHumanIds = snapshot.humans().stream()
                .limit(3)
                .map(SimulationReadModelQueryService.HumanProjection::id)
                .toList();
        response.getReferencedEntities().setHumanIds(topHumanIds);

        response.setMessage("Snapshot: tick " + snapshot.run().tick()
                + ", year " + snapshot.run().year()
                + ", population " + snapshot.metrics().population()
                + ", events " + snapshot.metrics().eventCount()
                + ", inventions " + snapshot.metrics().inventionCount() + ".");
    }

    private void executeSummary(Long cityId, AgentChatResponseOutput response) {
        SimulationApplicationService.TimelineHistory timeline =
                simulationApplicationService.listCityTimeline(cityId, null, null, 20);

        response.getExecutedActions().add(new AgentActionOutput(
                "READ_SUMMARY",
                "COMPLETED",
                "Summarized recent city changes from timeline history"
        ));
        response.getUiEffects().add(new AgentUiEffectOutput("REFRESH_TIMELINE"));

        List<Long> eventIds = timeline.events().stream().map(Event::getId).limit(3).toList();
        List<Long> inventionIds = timeline.inventions().stream().map(Invention::getId).limit(3).toList();
        response.getReferencedEntities().setEventIds(eventIds);
        response.getReferencedEntities().setInventionIds(inventionIds);

        String latestEvent = timeline.events().isEmpty()
                ? "none"
                : timeline.events().get(timeline.events().size() - 1).getEventType().name();
        String latestInvention = timeline.inventions().isEmpty()
                ? "none"
                : timeline.inventions().get(timeline.inventions().size() - 1).getTitle();

        response.setMessage("Recent summary: " + timeline.events().size() + " event(s), "
                + timeline.inventions().size() + " invention(s). Latest event: " + latestEvent
                + ". Latest invention: " + latestInvention + ".");
    }

    private void executeExplainEvent(
            Long cityId,
            AgentChatRequestInput input,
            String normalizedMessage,
            AgentChatResponseOutput response
    ) {
        List<Event> cityEvents = eventRepository.findByCityIdOrderByTickAscSequenceInTickAscIdAsc(cityId);
        if (cityEvents.isEmpty()) {
            response.getExecutedActions().add(new AgentActionOutput(
                    "EXPLAIN_EVENT",
                    "REJECTED",
                    "No events exist yet for this city"
            ));
            response.setMessage("There are no events yet to explain. Try stepping the simulation first.");
            return;
        }

        Event target = resolveTargetEvent(cityId, input, normalizedMessage, cityEvents);

        response.getExecutedActions().add(new AgentActionOutput(
                "EXPLAIN_EVENT",
                "COMPLETED",
                "Explained event " + target.getId()
        ));
        response.getReferencedEntities().setEventIds(List.of(target.getId()));

        AgentUiEffectOutput highlight = new AgentUiEffectOutput("HIGHLIGHT_EVENT");
        highlight.setEventId(target.getId());
        AgentUiEffectOutput panel = new AgentUiEffectOutput("SELECT_PANEL");
        panel.setPanel("events");
        response.getUiEffects().add(highlight);
        response.getUiEffects().add(panel);

        String narrative = target.getEnrichedSnippet();
        if (narrative == null || narrative.isBlank()) {
            narrative = "Event " + target.getEventType().name() + " occurred at tick " + target.getTick() + ".";
        }

        response.setMessage("Event " + target.getId() + ": " + narrative);
    }

    private void executeRecentInventions(Long cityId, AgentChatResponseOutput response) {
        List<Invention> inventions = inventionRepository.findByCityIdOrderByTickCreatedAscInventionKeyAscIdAsc(cityId);
        List<Invention> recent = takeLast(inventions, 5);

        response.getExecutedActions().add(new AgentActionOutput(
                "READ_INVENTIONS",
                "COMPLETED",
                "Loaded recent inventions"
        ));

        if (recent.isEmpty()) {
            response.setMessage("No inventions are recorded yet for this city.");
            return;
        }

        Invention latest = recent.get(recent.size() - 1);
        response.getReferencedEntities().setInventionIds(recent.stream().map(Invention::getId).toList());

        AgentUiEffectOutput highlight = new AgentUiEffectOutput("HIGHLIGHT_INVENTION");
        highlight.setInventionId(latest.getId());
        AgentUiEffectOutput panel = new AgentUiEffectOutput("SELECT_PANEL");
        panel.setPanel("inventions");
        response.getUiEffects().add(highlight);
        response.getUiEffects().add(panel);

        String names = recent.stream().map(Invention::getTitle).toList().toString();
        response.setMessage("Recent inventions (" + recent.size() + "): " + names);
    }

    private void executeFocusHuman(
            Long cityId,
            AgentChatRequestInput input,
            String normalizedMessage,
            AgentChatResponseOutput response
    ) {
        Human target = resolveTargetHuman(cityId, input, normalizedMessage);

        response.getExecutedActions().add(new AgentActionOutput(
                "FOCUS_HUMAN",
                "COMPLETED",
                "Focused on human " + target.getId()
        ));
        response.getReferencedEntities().setHumanIds(List.of(target.getId()));

        AgentUiEffectOutput focus = new AgentUiEffectOutput("FOCUS_HUMAN");
        focus.setHumanId(target.getId());
        response.getUiEffects().add(focus);
        response.setStructuredData(Map.of("focusHuman", toHumanSummary(target)));
        response.setMessage("Focused on " + target.getName() + " (human " + target.getId() + ").");
    }

    private void executeCompareHumans(
            Long cityId,
            AgentChatRequestInput input,
            String normalizedMessage,
            AgentChatResponseOutput response
    ) {
        List<Human> humans = humanRepository.findByCityIdOrderByIdAsc(cityId);
        if (humans.size() < 2) {
            response.getExecutedActions().add(new AgentActionOutput(
                    "COMPARE_HUMANS",
                    "REJECTED",
                    "At least two humans are required to compare"
            ));
            response.setMessage("I need at least two humans in this city to compare.");
            return;
        }

        List<Long> ids = extractNumbers(normalizedMessage);
        Human left = resolveHumanByIdOrDefault(humans, input.getSelectedHumanId(), ids, 0);
        Human right = resolveHumanByIdOrDefault(humans, null, ids, 1);
        if (left.getId().equals(right.getId())) {
            right = humans.stream().filter(h -> !h.getId().equals(left.getId())).findFirst().orElse(right);
        }

        response.getExecutedActions().add(new AgentActionOutput(
                "COMPARE_HUMANS",
                "COMPLETED",
                "Compared humans " + left.getId() + " and " + right.getId()
        ));
        response.getReferencedEntities().setHumanIds(List.of(left.getId(), right.getId()));

        AgentUiEffectOutput focus = new AgentUiEffectOutput("FOCUS_HUMAN");
        focus.setHumanId(left.getId());
        response.getUiEffects().add(focus);
        response.setStructuredData(Map.of(
                "compareHumans", Map.of(
                        "left", toHumanSummary(left),
                        "right", toHumanSummary(right)
                )
        ));
        response.setMessage("Compared " + left.getName() + " and " + right.getName()
                + ". Busy states: " + left.isBusy() + " vs " + right.isBusy() + ".");
    }

    private void executeFollowHuman(
            Long cityId,
            AgentChatRequestInput input,
            String normalizedMessage,
            AgentChatResponseOutput response
    ) {
        Human target = resolveTargetHuman(cityId, input, normalizedMessage);
        int followTicks = Math.max(1, Math.min(extractRequestedFollowTickCount(normalizedMessage), 20));
        SimulationRun run = simulationApplicationService.step(cityId, followTicks);
        long fromTick = Math.max(1L, run.getTick() - followTicks + 1);
        List<Event> followEvents = eventRepository.findHumanEventsInTickWindow(
                cityId,
                target.getId(),
                fromTick,
                run.getTick()
        );
        List<Invention> followInventions = inventionRepository.findByCityIdAndTickCreatedBetweenOrderByTickCreatedAscInventionKeyAscIdAsc(
                cityId,
                fromTick,
                run.getTick()
        );

        response.getExecutedActions().add(new AgentActionOutput(
                "FOLLOW_HUMAN",
                "COMPLETED",
                "Followed human " + target.getId() + " for " + followTicks + " tick(s)"
        ));
        response.getReferencedEntities().setHumanIds(List.of(target.getId()));
        response.getReferencedEntities().setEventIds(takeLast(followEvents, 5).stream().map(Event::getId).toList());
        response.getReferencedEntities().setInventionIds(takeLast(followInventions, 5).stream().map(Invention::getId).toList());

        AgentUiEffectOutput focus = new AgentUiEffectOutput("FOCUS_HUMAN");
        focus.setHumanId(target.getId());
        response.getUiEffects().add(focus);
        response.getUiEffects().add(new AgentUiEffectOutput("REFRESH_SNAPSHOT"));
        AgentUiEffectOutput refreshTimeline = new AgentUiEffectOutput("REFRESH_TIMELINE");
        refreshTimeline.setFromTick(fromTick);
        response.getUiEffects().add(refreshTimeline);
        if (!followEvents.isEmpty()) {
            Event latestEvent = followEvents.get(followEvents.size() - 1);
            AgentUiEffectOutput highlightEvent = new AgentUiEffectOutput("HIGHLIGHT_EVENT");
            highlightEvent.setEventId(latestEvent.getId());
            response.getUiEffects().add(highlightEvent);
        }
        if (!followInventions.isEmpty()) {
            Invention latestInvention = followInventions.get(followInventions.size() - 1);
            AgentUiEffectOutput highlightInvention = new AgentUiEffectOutput("HIGHLIGHT_INVENTION");
            highlightInvention.setInventionId(latestInvention.getId());
            response.getUiEffects().add(highlightInvention);
        }
        response.setStructuredData(Map.of(
                "followHuman", Map.of(
                        "human", toHumanSummary(target),
                        "ticks", followTicks,
                        "fromTick", fromTick,
                        "resultTick", run.getTick(),
                        "eventWindow", takeLast(followEvents, 8).stream().map(this::toFollowEventSummary).toList(),
                        "inventionWindow", takeLast(followInventions, 8).stream().map(this::toFollowInventionSummary).toList()
                )
        ));
        response.setMessage("Followed " + target.getName() + " for " + followTicks
                + " tick(s). Simulation is now at tick " + run.getTick() + ".");
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

    private Event resolveTargetEvent(
            Long cityId,
            AgentChatRequestInput input,
            String normalizedMessage,
            List<Event> cityEvents
    ) {
        List<Long> candidateIds = new ArrayList<>();
        if (input.getSelectedEventId() != null) {
            candidateIds.add(input.getSelectedEventId());
        }
        extractFirstNumber(normalizedMessage).ifPresent(candidateIds::add);

        for (Long candidateId : candidateIds) {
            Event candidate = eventRepository.findById(candidateId).orElse(null);
            if (candidate != null && candidate.getCity().getId().equals(cityId)) {
                return candidate;
            }
        }

        return cityEvents.stream()
                .max(Comparator.comparing(Event::getTick)
                        .thenComparing(Event::getSequenceInTick)
                        .thenComparing(Event::getId))
                .orElseThrow();
    }

    private String classifyIntent(String normalizedMessage) {
        if (containsAny(normalizedMessage, "compare", "versus", " vs ")) {
            return "compare_humans";
        }
        if (containsAny(normalizedMessage, "follow", "track")) {
            return "follow_human";
        }
        if (containsAny(normalizedMessage, "focus", "inspect")) {
            return "focus_human";
        }
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

    private int extractRequestedStepCount(String message) {
        return extractFirstNumber(message)
                .map(number -> Math.toIntExact(Math.min(number, Integer.MAX_VALUE)))
                .orElse(1);
    }

    private int extractRequestedFollowTickCount(String message) {
        Matcher matcher = FOLLOW_TICKS_PATTERN.matcher(message);
        if (matcher.find()) {
            return Math.toIntExact(Math.min(Long.parseLong(matcher.group(1)), Integer.MAX_VALUE));
        }
        List<Long> numbers = extractNumbers(message);
        if (numbers.size() >= 2) {
            return Math.toIntExact(Math.min(numbers.get(numbers.size() - 1), Integer.MAX_VALUE));
        }
        return numbers.isEmpty() ? 1 : Math.toIntExact(Math.min(numbers.get(0), Integer.MAX_VALUE));
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

    private Human resolveTargetHuman(Long cityId, AgentChatRequestInput input, String normalizedMessage) {
        List<Human> humans = humanRepository.findByCityIdOrderByIdAsc(cityId);
        if (humans.isEmpty()) {
            throw new IllegalArgumentException("No humans found for this city");
        }
        List<Long> ids = extractNumbers(normalizedMessage);
        return resolveHumanByIdOrDefault(humans, input.getSelectedHumanId(), ids, 0);
    }

    private Human resolveHumanByIdOrDefault(
            List<Human> humans,
            Long preferredId,
            List<Long> parsedIds,
            int parsedIndex
    ) {
        if (preferredId != null) {
            Human preferred = humans.stream().filter(h -> h.getId().equals(preferredId)).findFirst().orElse(null);
            if (preferred != null) {
                return preferred;
            }
        }
        if (parsedIds.size() > parsedIndex) {
            Long parsedId = parsedIds.get(parsedIndex);
            Human parsed = humans.stream().filter(h -> h.getId().equals(parsedId)).findFirst().orElse(null);
            if (parsed != null) {
                return parsed;
            }
        }
        return humans.get(0);
    }

    private Map<String, Object> toHumanSummary(Human human) {
        return Map.of(
                "id", human.getId(),
                "name", human.getName(),
                "busy", human.isBusy(),
                "x", human.getX() == null ? 0.0 : human.getX(),
                "y", human.getY() == null ? 0.0 : human.getY()
        );
    }

    private Map<String, Object> toFollowEventSummary(Event event) {
        return Map.of(
                "id", event.getId(),
                "tick", event.getTick(),
                "type", event.getEventType().name(),
                "year", event.getYear()
        );
    }

    private Map<String, Object> toFollowInventionSummary(Invention invention) {
        return Map.of(
                "id", invention.getId(),
                "tickCreated", invention.getTickCreated(),
                "title", invention.getTitle(),
                "category", invention.getCategory().name()
        );
    }

    private <T> List<T> takeLast(List<T> input, int max) {
        if (input.size() <= max) {
            return input;
        }
        return input.subList(input.size() - max, input.size());
    }
}
