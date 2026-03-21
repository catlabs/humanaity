package eu.catlabs.humanaity.simulation.application.assistant;

import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.domain.EventType;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.history.domain.HistoryEra;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.invention.domain.Invention;
import eu.catlabs.humanaity.invention.infrastructure.persistence.InventionRepository;
import eu.catlabs.humanaity.simulation.api.dto.SimulationAssistantBlockOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationAssistantItemOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationAssistantMetricOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationAssistantRequestInput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationAssistantResponseOutput;
import eu.catlabs.humanaity.simulation.application.query.SimulationReadModelQueryService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class SimulationAssistantService {

    private static final int RECENT_EVENTS_LIMIT = 8;
    private static final int RECENT_INVENTIONS_LIMIT = 8;
    private static final int RELATIONSHIP_LIMIT = 8;

    private final CityRepository cityRepository;
    private final HumanRepository humanRepository;
    private final EventRepository eventRepository;
    private final InventionRepository inventionRepository;
    private final SimulationReadModelQueryService simulationReadModelQueryService;
    private final SimulationAssistantCommandInterpreter commandInterpreter;

    public SimulationAssistantService(
            CityRepository cityRepository,
            HumanRepository humanRepository,
            EventRepository eventRepository,
            InventionRepository inventionRepository,
            SimulationReadModelQueryService simulationReadModelQueryService,
            SimulationAssistantCommandInterpreter commandInterpreter
    ) {
        this.cityRepository = cityRepository;
        this.humanRepository = humanRepository;
        this.eventRepository = eventRepository;
        this.inventionRepository = inventionRepository;
        this.simulationReadModelQueryService = simulationReadModelQueryService;
        this.commandInterpreter = commandInterpreter;
    }

    @Transactional(readOnly = true)
    public SimulationAssistantResponseOutput handle(Long cityId, User currentUser, SimulationAssistantRequestInput input) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + cityId));
        ensureOwnership(city, currentUser);

        String commandText = input == null || input.getCommandText() == null
                ? ""
                : input.getCommandText().trim();
        SimulationAssistantCommandInterpretation interpretation = commandInterpreter.interpret(commandText);

        return switch (interpretation.commandType()) {
            case INVENTIONS -> inventionsResponse(cityId);
            case WORLD_STATUS -> worldStatusResponse(cityId);
            case RECENT_EVENTS -> recentEventsResponse(cityId);
            case RELATIONSHIPS -> relationshipsResponse(cityId);
            case UNSUPPORTED -> unsupportedResponse();
        };
    }

    private SimulationAssistantResponseOutput inventionsResponse(Long cityId) {
        List<Invention> inventions = inventionRepository.findByCityIdOrderByTickCreatedAscInventionKeyAscIdAsc(cityId);
        List<Invention> recentInventions = reverseCopy(takeLast(inventions, RECENT_INVENTIONS_LIMIT));

        SimulationAssistantBlockOutput block = new SimulationAssistantBlockOutput();
        block.setType("INVENTIONS");
        block.setTitle("Inventions");
        block.setSubtitle("Latest unlocked inventions in deterministic order.");
        block.setMetrics(List.of(
                metric("Total", String.valueOf(inventions.size())),
                metric("Techniques", String.valueOf(inventions.stream().filter(inv -> "TECHNIQUE".equals(inv.getCategory().name())).count())),
                metric("Knowledge", String.valueOf(inventions.stream().filter(inv -> "KNOWLEDGE".equals(inv.getCategory().name())).count())),
                metric("Social", String.valueOf(inventions.stream().filter(inv -> "SOCIAL_PRACTICE".equals(inv.getCategory().name())).count()))
        ));
        block.setItems(recentInventions.stream()
                .map(invention -> new SimulationAssistantItemOutput(
                        invention.getTitle(),
                        "Year " + invention.getYearCreated() + " · " + formatEnumLabel(invention.getEraCreated()) + " · Impact " + invention.getImpactScore(),
                        invention.getSummary(),
                        List.of(formatEnumLabel(invention.getCategory()), "Tick " + invention.getTickCreated())
                ))
                .toList());
        block.setEmptyState("No inventions unlocked yet.");

        SimulationAssistantResponseOutput response = baseResponse(true, SimulationAssistantCommandType.INVENTIONS);
        response.setText(inventions.isEmpty()
                ? "No inventions are available yet."
                : "Here is the current invention inventory for this simulation.");
        response.setBlocks(List.of(block));
        return response;
    }

    private SimulationAssistantResponseOutput worldStatusResponse(Long cityId) {
        SimulationReadModelQueryService.SimulationSnapshotProjection snapshot =
                simulationReadModelQueryService.getCitySnapshot(cityId, ignored -> false);

        SimulationAssistantBlockOutput block = new SimulationAssistantBlockOutput();
        block.setType("WORLD_STATUS");
        block.setTitle("World status");
        block.setSubtitle("Backend-owned summary of the current simulation state.");
        block.setMetrics(List.of(
                metric("Run", snapshot.run().hasRun() ? (snapshot.run().running() ? "Running" : "Paused") : "Not started"),
                metric("Tick", String.valueOf(snapshot.run().tick())),
                metric("Year", String.valueOf(snapshot.run().year())),
                metric("Era", formatEnumLabel(snapshot.run().era())),
                metric("Population", String.valueOf(snapshot.metrics().population())),
                metric("Busy", String.valueOf(snapshot.metrics().busyCount())),
                metric("Events", String.valueOf(snapshot.metrics().eventCount())),
                metric("Inventions", String.valueOf(snapshot.metrics().inventionCount()))
        ));
        block.setItems(List.of(
                new SimulationAssistantItemOutput(
                        snapshot.city().name(),
                        "City overview",
                        buildWorldStatusBody(snapshot),
                        List.of(
                                snapshot.run().status() == null ? "Created" : formatEnumLabel(snapshot.run().status()),
                                "Recent events " + snapshot.timelineSummary().recentEventCount(),
                                "Recent inventions " + snapshot.timelineSummary().recentInventionCount()
                        )
                )
        ));

        SimulationAssistantResponseOutput response = baseResponse(true, SimulationAssistantCommandType.WORLD_STATUS);
        response.setText("Here is the current global status of the simulation.");
        response.setBlocks(List.of(block));
        return response;
    }

    private SimulationAssistantResponseOutput recentEventsResponse(Long cityId) {
        List<Event> events = eventRepository.findByCityIdOrderByTickAscSequenceInTickAscIdAsc(cityId);
        List<Event> recentEvents = reverseCopy(takeLast(events, RECENT_EVENTS_LIMIT));

        SimulationAssistantBlockOutput block = new SimulationAssistantBlockOutput();
        block.setType("RECENT_EVENTS");
        block.setTitle("Recent events");
        block.setSubtitle("Most recent deterministic history entries.");
        block.setMetrics(List.of(
                metric("Total", String.valueOf(events.size())),
                metric("Visible", String.valueOf(recentEvents.size()))
        ));
        block.setItems(recentEvents.stream()
                .map(event -> new SimulationAssistantItemOutput(
                        formatEnumLabel(event.getEventType()),
                        "Tick " + event.getTick() + " · Year " + event.getYear() + " · " + formatEnumLabel(event.getEra()),
                        describeEvent(event, cityId),
                        buildEventChips(event)
                ))
                .toList());
        block.setEmptyState("No events recorded yet.");

        SimulationAssistantResponseOutput response = baseResponse(true, SimulationAssistantCommandType.RECENT_EVENTS);
        response.setText(recentEvents.isEmpty()
                ? "No recent events are available yet."
                : "Here are the latest events from the simulation timeline.");
        response.setBlocks(List.of(block));
        return response;
    }

    private SimulationAssistantResponseOutput relationshipsResponse(Long cityId) {
        List<Human> humans = humanRepository.findByCityIdOrderByIdAsc(cityId);
        Map<Long, String> humanNames = humans.stream()
                .collect(LinkedHashMap::new, (map, human) -> map.put(human.getId(), human.getName()), Map::putAll);

        List<RelationshipSummary> relationships = eventRepository
                .findByCityIdOrderByTickAscSequenceInTickAscIdAsc(cityId)
                .stream()
                .filter(event -> event.getEventType() == EventType.HUMANS_COLLIDED || event.getEventType() == EventType.DIALOGUE_EXCHANGED)
                .map(event -> toPairEvent(event))
                .filter(Objects::nonNull)
                .collect(LinkedHashMap<PairKey, RelationshipSummary>::new,
                        (map, pairEvent) -> map.merge(
                                pairEvent.key(),
                                new RelationshipSummary(pairEvent.key(), pairEvent.dialogueCount(), pairEvent.collisionCount(), pairEvent.latestTick()),
                                RelationshipSummary::merge
                        ),
                        Map::putAll)
                .values()
                .stream()
                .sorted(Comparator
                        .comparingInt(RelationshipSummary::totalInteractions).reversed()
                        .thenComparingLong(RelationshipSummary::latestTick).reversed())
                .limit(RELATIONSHIP_LIMIT)
                .toList();

        SimulationAssistantBlockOutput block = new SimulationAssistantBlockOutput();
        block.setType("RELATIONSHIPS");
        block.setTitle("Relationships");
        block.setSubtitle("Interaction pairs derived from dialogue and collision events.");
        block.setMetrics(List.of(
                metric("Population", String.valueOf(humans.size())),
                metric("Pairs", String.valueOf(relationships.size()))
        ));
        block.setItems(relationships.stream()
                .map(relationship -> new SimulationAssistantItemOutput(
                        relationship.key().displayName(humanNames),
                        "Last interaction at tick " + relationship.latestTick(),
                        relationship.dialogueCount() + " dialogues · " + relationship.collisionCount() + " collisions",
                        List.of(
                                "Interactions " + relationship.totalInteractions(),
                                "Latest tick " + relationship.latestTick()
                        )
                ))
                .toList());
        block.setEmptyState("No recurring relationships can be derived yet.");

        SimulationAssistantResponseOutput response = baseResponse(true, SimulationAssistantCommandType.RELATIONSHIPS);
        response.setText(relationships.isEmpty()
                ? "No relationships are visible yet from simulation interactions."
                : "Here is the current interaction view derived from recent simulation history.");
        response.setBlocks(List.of(block));
        return response;
    }

    private SimulationAssistantResponseOutput unsupportedResponse() {
        SimulationAssistantBlockOutput block = new SimulationAssistantBlockOutput();
        block.setType("SUPPORTED_COMMANDS");
        block.setTitle("Supported commands");
        block.setSubtitle("Use one of the deterministic assistant commands below.");
        block.setItems(List.of(
                new SimulationAssistantItemOutput("inventions", "Inventory", "Read the current invention inventory.", List.of()),
                new SimulationAssistantItemOutput("world status", "Summary", "Read the current global state of the simulation.", List.of()),
                new SimulationAssistantItemOutput("recent events", "Timeline", "Read the most recent history entries.", List.of()),
                new SimulationAssistantItemOutput("relationships", "Interactions", "Read the strongest interaction pairs.", List.of())
        ));

        SimulationAssistantResponseOutput response = baseResponse(false, SimulationAssistantCommandType.UNSUPPORTED);
        response.setText("Unsupported command. Use one of the predefined deterministic commands.");
        response.setBlocks(List.of(block));
        return response;
    }

    private SimulationAssistantResponseOutput baseResponse(boolean ok, SimulationAssistantCommandType commandType) {
        SimulationAssistantResponseOutput response = new SimulationAssistantResponseOutput();
        response.setOk(ok);
        response.setCommandType(commandType.name());
        return response;
    }

    private SimulationAssistantMetricOutput metric(String label, String value) {
        return new SimulationAssistantMetricOutput(label, value);
    }

    private String buildWorldStatusBody(SimulationReadModelQueryService.SimulationSnapshotProjection snapshot) {
        HistoryEra era = snapshot.run().era();
        String runState = snapshot.run().hasRun()
                ? (snapshot.run().running() ? "running" : "paused")
                : "not started";
        return "The simulation is " + runState
                + ", currently at year " + snapshot.run().year()
                + " in the " + formatEnumLabel(era)
                + " era, with " + snapshot.metrics().population()
                + " humans and " + snapshot.metrics().eventCount()
                + " recorded events.";
    }

    private String describeEvent(Event event, Long cityId) {
        List<String> names = resolveActorNames(cityId, event.getActorIds());
        String actorSummary = names.isEmpty() ? "No actors" : String.join(" · ", names);
        String payloadSummary = event.getPayload() == null || event.getPayload().isEmpty()
                ? ""
                : " · " + event.getPayload().entrySet().stream()
                .limit(2)
                .map(entry -> formatEnumLabel(entry.getKey()) + ": " + entry.getValue())
                .reduce((left, right) -> left + " · " + right)
                .orElse("");
        return actorSummary + payloadSummary;
    }

    private List<String> buildEventChips(Event event) {
        List<String> chips = new ArrayList<>();
        chips.add(formatEnumLabel(event.getEventCategory()));
        chips.add("Importance " + event.getImportance());
        if (event.getActorIds() != null && !event.getActorIds().isEmpty()) {
            chips.add(event.getActorIds().size() + " actors");
        }
        return chips;
    }

    private List<String> resolveActorNames(Long cityId, List<Long> actorIds) {
        if (actorIds == null || actorIds.isEmpty()) {
            return List.of();
        }
        Map<Long, String> byId = humanRepository.findByCityIdOrderByIdAsc(cityId).stream()
                .collect(LinkedHashMap::new, (map, human) -> map.put(human.getId(), human.getName()), Map::putAll);
        return actorIds.stream()
                .map(actorId -> byId.getOrDefault(actorId, "Human " + actorId))
                .toList();
    }

    private PairEvent toPairEvent(Event event) {
        if (event.getActorIds() == null || event.getActorIds().size() < 2) {
            return null;
        }
        long left = Math.min(event.getActorIds().get(0), event.getActorIds().get(1));
        long right = Math.max(event.getActorIds().get(0), event.getActorIds().get(1));
        PairKey key = new PairKey(left, right);
        return switch (event.getEventType()) {
            case DIALOGUE_EXCHANGED -> new PairEvent(key, 1, 0, event.getTick());
            case HUMANS_COLLIDED -> new PairEvent(key, 0, 1, event.getTick());
            default -> null;
        };
    }

    private <T> List<T> reverseCopy(List<T> values) {
        List<T> copy = new ArrayList<>(values);
        java.util.Collections.reverse(copy);
        return copy;
    }

    private <T> List<T> takeLast(List<T> values, int limit) {
        if (values.size() <= limit) {
            return values;
        }
        return values.subList(values.size() - limit, values.size());
    }

    private String formatEnumLabel(Enum<?> value) {
        if (value == null) {
            return "Unknown";
        }
        return formatEnumLabel(value.name());
    }

    private String formatEnumLabel(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .trim()
                .replaceAll("\\s+", " ")
                .transform(label -> {
                    String[] parts = label.split(" ");
                    StringBuilder builder = new StringBuilder();
                    for (int index = 0; index < parts.length; index += 1) {
                        if (index > 0) {
                            builder.append(' ');
                        }
                        String part = parts[index];
                        builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
                    }
                    return builder.toString();
                });
    }

    private void ensureOwnership(City city, User currentUser) {
        if (city.getOwner() == null || currentUser == null || !city.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("City does not belong to current user");
        }
    }

    private record PairKey(long leftHumanId, long rightHumanId) {
        String displayName(Map<Long, String> humanNames) {
            return humanNames.getOrDefault(leftHumanId, "Human " + leftHumanId)
                    + " ↔ "
                    + humanNames.getOrDefault(rightHumanId, "Human " + rightHumanId);
        }
    }

    private record PairEvent(PairKey key, int dialogueCount, int collisionCount, long latestTick) {
    }

    private record RelationshipSummary(PairKey key, int dialogueCount, int collisionCount, long latestTick) {
        RelationshipSummary merge(RelationshipSummary other) {
            return new RelationshipSummary(
                    key,
                    dialogueCount + other.dialogueCount,
                    collisionCount + other.collisionCount,
                    Math.max(latestTick, other.latestTick)
            );
        }

        int totalInteractions() {
            return dialogueCount + collisionCount;
        }
    }
}
