package eu.catlabs.humanaity.simulation.application;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.application.EventApplicationService;
import eu.catlabs.humanaity.event.application.EventDraft;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.domain.EventType;
import eu.catlabs.humanaity.human.application.HumanApplicationService;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.invention.application.InventionApplicationService;
import eu.catlabs.humanaity.invention.domain.Invention;
import eu.catlabs.humanaity.invention.domain.InventionCategory;
import eu.catlabs.humanaity.simulation.application.query.SimulationReadModelQueryService;
import eu.catlabs.humanaity.simulation.domain.SimulationRun;
import eu.catlabs.humanaity.simulation.domain.SimulationRunStatus;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.SimulationRunRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

@Service
public class SimulationApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(SimulationApplicationService.class);
    private static final int MAX_STEPS_PER_REQUEST = 10_000;
    private static final double COLLISION_DISTANCE_THRESHOLD = 0.08;
    private static final long RECENT_DIALOGUE_WINDOW_TICKS = 3L;
    private static final long RECENT_COLLISION_DISCOVERY_WINDOW_TICKS = 6L;
    private static final long REACHED_PLACE_COOLDOWN_TICKS = 5L;
    private static final int STAYED_AT_PLACE_THRESHOLD_TICKS = 3;
    private static final long STAYED_AT_PLACE_COOLDOWN_TICKS = 3L;
    private static final double PROXIMITY_GROUP_DISTANCE_THRESHOLD = 0.12;
    private static final int PROXIMITY_GROUP_SUSTAIN_TICKS = 2;
    private static final long PROXIMITY_GROUP_COOLDOWN_TICKS = 4L;
    private static final long DISCOVERY_SEED_SALT = 0x9E3779B97F4A7C15L;
    private static final String[] INVENTION_TOPICS = {
            "Canal Irrigation",
            "Ceramic Kiln",
            "Signal Fire Chain",
            "Crop Rotation",
            "Public Assembly",
            "Oral Archive"
    };
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    private final Map<Long, ScheduledFuture<?>> runningTasks = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, String> lastPlaceByHuman = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Integer> placeStreakByHuman = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Long, java.util.Set<String>> previousProximityGroupsByCity = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Integer> proximityGroupStreakByKey = new java.util.concurrent.ConcurrentHashMap<>();
    private final HumanRepository humanRepository;
    private final HumanApplicationService humanApplicationService;
    private final CityRepository cityRepository;
    private final SimulationRunRepository simulationRunRepository;
    private final EventApplicationService eventApplicationService;
    private final InventionApplicationService inventionApplicationService;
    private final SimulationReadModelQueryService simulationReadModelQueryService;

    public SimulationApplicationService(
            HumanRepository humanRepository,
            HumanApplicationService humanApplicationService,
            CityRepository cityRepository,
            SimulationRunRepository simulationRunRepository,
            EventApplicationService eventApplicationService,
            InventionApplicationService inventionApplicationService,
            SimulationReadModelQueryService simulationReadModelQueryService
    ) {
        this.humanRepository = humanRepository;
        this.humanApplicationService = humanApplicationService;
        this.cityRepository = cityRepository;
        this.simulationRunRepository = simulationRunRepository;
        this.eventApplicationService = eventApplicationService;
        this.inventionApplicationService = inventionApplicationService;
        this.simulationReadModelQueryService = simulationReadModelQueryService;
    }

    public synchronized String startSimulation(Long cityId) {
        if (runningTasks.containsKey(cityId)) {
            return "Simulation already running for city " + cityId;
        }
        SimulationRun run = simulationRunRepository.findByCityId(cityId)
                .orElseGet(() -> createRun(cityId, ThreadLocalRandom.current().nextLong()));
        run.setStatus(SimulationRunStatus.RUNNING);
        SimulationRun savedRun = simulationRunRepository.save(run);
        eventApplicationService.emitLifecycleEvent(
                cityId,
                savedRun.getTick(),
                EventType.SIMULATION_STARTED,
                List.of(),
                Map.of("status", SimulationRunStatus.RUNNING.name()),
                10
        );

        ScheduledFuture<?> task = executor.scheduleAtFixedRate(
                () -> runScheduledStep(cityId),
                0,
                100,
                TimeUnit.MILLISECONDS
        );

        runningTasks.put(cityId, task);
        return "Simulation started for city " + cityId;
    }

    public synchronized String stopSimulation(Long cityId) {
        ScheduledFuture<?> task = runningTasks.get(cityId);
        if (task == null) {
            return "No simulation running for city " + cityId;
        }

        task.cancel(true);
        runningTasks.remove(cityId);
        SimulationRun run = loadRun(cityId);
        run.setStatus(SimulationRunStatus.COMPLETED);
        SimulationRun savedRun = simulationRunRepository.save(run);
        eventApplicationService.emitLifecycleEvent(
                cityId,
                savedRun.getTick(),
                EventType.SIMULATION_COMPLETED,
                List.of(),
                Map.of("status", SimulationRunStatus.COMPLETED.name()),
                20
        );
        return "Simulation stopped for city " + cityId;
    }

    public boolean isRunning(Long cityId) {
        return runningTasks.containsKey(cityId);
    }

    public SimulationRun createRun(Long cityId) {
        return createRun(cityId, ThreadLocalRandom.current().nextLong());
    }

    public SimulationRun createRun(Long cityId, Long seed) {
        Optional<SimulationRun> existing = simulationRunRepository.findByCityId(cityId);
        if (existing.isPresent()) {
            return existing.get();
        }

        City city = getCityOrThrow(cityId);
        SimulationRun run = new SimulationRun();
        run.setCity(city);
        run.setSeed(seed);
        run.setTick(0L);
        run.setStatus(SimulationRunStatus.CREATED);
        return simulationRunRepository.save(run);
    }

    public SimulationRun loadRun(Long cityId) {
        return simulationRunRepository.findByCityId(cityId)
                .orElseThrow(() -> new EntityNotFoundException("Simulation run not found for city " + cityId));
    }

    public synchronized SimulationRun pauseRun(Long cityId) {
        SimulationRun run = loadRun(cityId);
        run.setStatus(SimulationRunStatus.PAUSED);
        SimulationRun savedRun = simulationRunRepository.save(run);
        eventApplicationService.emitLifecycleEvent(
                cityId,
                savedRun.getTick(),
                EventType.SIMULATION_PAUSED,
                List.of(),
                Map.of("status", SimulationRunStatus.PAUSED.name()),
                10
        );
        return savedRun;
    }

    public synchronized SimulationRun resumeRun(Long cityId) {
        SimulationRun run = simulationRunRepository.findByCityId(cityId)
                .orElseGet(() -> createRun(cityId, ThreadLocalRandom.current().nextLong()));
        run.setStatus(SimulationRunStatus.RUNNING);
        SimulationRun savedRun = simulationRunRepository.save(run);
        eventApplicationService.emitLifecycleEvent(
                cityId,
                savedRun.getTick(),
                EventType.SIMULATION_RESUMED,
                List.of(),
                Map.of("status", SimulationRunStatus.RUNNING.name()),
                10
        );
        return savedRun;
    }

    @Transactional
    public synchronized SimulationRun step(Long cityId) {
        return step(cityId, 1);
    }

    @Transactional
    public synchronized SimulationRun step(Long cityId, int count) {
        if (count <= 0 || count > MAX_STEPS_PER_REQUEST) {
            throw new IllegalArgumentException("count must be between 1 and " + MAX_STEPS_PER_REQUEST);
        }

        SimulationRun run = simulationRunRepository.findByCityId(cityId)
                .orElseGet(() -> createRun(cityId, ThreadLocalRandom.current().nextLong()));

        for (int i = 0; i < count; i++) {
            runSingleDeterministicTick(run);
        }

        return run;
    }

    public synchronized SimulationRun stepViaSchedulerWrapper(Long cityId, int count) {
        if (count <= 0 || count > MAX_STEPS_PER_REQUEST) {
            throw new IllegalArgumentException("count must be between 1 and " + MAX_STEPS_PER_REQUEST);
        }

        for (int i = 0; i < count; i++) {
            runScheduledStep(cityId);
        }

        return loadRun(cityId);
    }

    public List<Event> listCityEvents(Long cityId, Long fromTick, Long toTick, Integer limit) {
        return eventApplicationService.listCityEvents(cityId, fromTick, toTick, limit);
    }

    public List<Invention> listCityInventions(Long cityId, Long fromTick, Long toTick, Integer limit) {
        return inventionApplicationService.listCityInventions(cityId, fromTick, toTick, limit);
    }

    public TimelineHistory listCityTimeline(Long cityId, Long fromTick, Long toTick, Integer limit) {
        List<Event> events = listCityEvents(cityId, fromTick, toTick, limit);
        List<Invention> inventions = listCityInventions(cityId, fromTick, toTick, limit);

        Long effectiveToTick = toTick;
        if (effectiveToTick == null) {
            Long latestEventTick = events.isEmpty() ? null : events.get(events.size() - 1).getTick();
            Long latestInventionTick = inventions.isEmpty() ? null : inventions.get(inventions.size() - 1).getTickCreated();
            if (latestEventTick == null) {
                effectiveToTick = latestInventionTick;
            } else if (latestInventionTick == null) {
                effectiveToTick = latestEventTick;
            } else {
                effectiveToTick = Math.max(latestEventTick, latestInventionTick);
            }
        }

        return new TimelineHistory(
                cityId,
                fromTick == null ? 0L : fromTick,
                effectiveToTick,
                events,
                inventions
        );
    }

    public List<SimulationReadModelQueryService.CityOverviewProjection> listCityOverviews() {
        Predicate<Long> runningLookup = this::isRunning;
        return simulationReadModelQueryService.listCityOverviews(runningLookup);
    }

    public SimulationReadModelQueryService.SimulationSnapshotProjection getCitySnapshot(Long cityId) {
        Predicate<Long> runningLookup = this::isRunning;
        return simulationReadModelQueryService.getCitySnapshot(cityId, runningLookup);
    }

    private City getCityOrThrow(Long cityId) {
        Objects.requireNonNull(cityId, "cityId must not be null");
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + cityId));
    }

    private void runScheduledStep(Long cityId) {
        try {
            step(cityId);
        } catch (Exception e) {
            logger.error("Error simulating city {}: {}", cityId, e.getMessage(), e);
        }
    }

    private void runSingleDeterministicTick(SimulationRun run) {
        if (run.getTick() == 0L) {
            resetPlaceStayState(run.getCity().getId());
            resetProximityGroupState(run.getCity().getId());
        }
        List<Human> orderedHumans = new ArrayList<>(humanRepository.findByCityIdOrderByIdAsc(run.getCity().getId()));
        if (orderedHumans.isEmpty()) {
            logger.debug("No humans found in city {}", run.getCity().getId());
            long nextTick = run.getTick() + 1;
            run.setTick(nextTick);
            simulationRunRepository.save(run);
            emitMilestoneEvents(run.getCity().getId(), inventionApplicationService.deriveFromPersistedEvents(run.getCity().getId()));
            return;
        }

        Map<Long, String> previousPlaceByHuman = new HashMap<>();
        for (Human human : orderedHumans) {
            previousPlaceByHuman.put(
                    human.getId(),
                    resolvePlaceForPosition(human.getX(), human.getY()).map(SimulationPlaceRegistry.SimulationPlace::id).orElse(null)
            );
        }

        for (Human human : orderedHumans) {
            human.setBusy(false);
            updateHumanPosition(run.getSeed(), run.getTick(), human);
        }

        humanRepository.saveAll(orderedHumans);
        humanApplicationService.publishHumanUpdates(orderedHumans);

        long nextTick = run.getTick() + 1;
        run.setTick(nextTick);
        simulationRunRepository.save(run);

        List<EventDraft> stepEvents = buildStepEventDrafts(run, nextTick, orderedHumans, previousPlaceByHuman);
        eventApplicationService.emitEventsAtTick(run.getCity().getId(), nextTick, stepEvents);

        List<Invention> createdInventions = inventionApplicationService.deriveFromPersistedEvents(run.getCity().getId());
        emitMilestoneEvents(run.getCity().getId(), createdInventions);
    }

    private void updateHumanPosition(Long runSeed, Long tick, Human human) {
        Random tickHumanRandom = new Random(deriveDeterministicSeed(runSeed, tick, human.getId()));
        double deltaX = (tickHumanRandom.nextDouble() - 0.5) * 0.1;
        double deltaY = (tickHumanRandom.nextDouble() - 0.5) * 0.1;

        double newX = Math.max(0, Math.min(1, human.getX() + deltaX));
        double newY = Math.max(0, Math.min(1, human.getY() + deltaY));

        human.setX(newX);
        human.setY(newY);
    }

    private long deriveDeterministicSeed(Long runSeed, Long tick, Long humanId) {
        long mixed = runSeed;
        mixed = 31L * mixed + tick;
        mixed = 31L * mixed + humanId;
        return mixed;
    }

    private List<EventDraft> buildStepEventDrafts(
            SimulationRun run,
            long tick,
            List<Human> orderedHumans,
            Map<Long, String> previousPlaceByHuman
    ) {
        List<Human> humans = orderedHumans.stream()
                .sorted(Comparator.comparing(Human::getId))
                .toList();
        List<EventDraft> drafts = new ArrayList<>();
        drafts.addAll(buildCollisionDrafts(tick, humans));
        drafts.addAll(buildDialogueDrafts(run.getCity().getId(), tick, humans));
        drafts.addAll(buildReachedPlaceDrafts(run.getCity().getId(), tick, humans, previousPlaceByHuman));
        drafts.addAll(buildStayedAtPlaceDrafts(run.getCity().getId(), tick, humans));
        List<EventDraft> proximityGroupDrafts = buildProximityGroupDrafts(run.getCity().getId(), tick, humans);
        drafts.addAll(proximityGroupDrafts);
        List<EventDraft> collisionDiscoveries = buildCollisionDiscoveryDrafts(run.getCity().getId(), tick, humans);
        drafts.addAll(collisionDiscoveries);
        List<EventDraft> contextDiscoveries = new ArrayList<>(collisionDiscoveries);
        for (EventDraft draft : proximityGroupDrafts) {
            if (draft.eventType() == EventType.DISCOVERY_UNLOCKED) {
                contextDiscoveries.add(draft);
            }
        }
        drafts.addAll(buildDiscoveryDrafts(run.getSeed(), tick, humans, humansWithContextDiscovery(contextDiscoveries)));
        return drafts;
    }

    private List<EventDraft> buildCollisionDrafts(long tick, List<Human> humans) {
        List<EventDraft> drafts = new ArrayList<>();
        for (int i = 0; i < humans.size(); i++) {
            Human left = humans.get(i);
            for (int j = i + 1; j < humans.size(); j++) {
                Human right = humans.get(j);
                double distance = distance(left, right);
                if (distance > COLLISION_DISTANCE_THRESHOLD) {
                    continue;
                }
                String collisionKey = left.getId() + "-" + right.getId() + "-" + tick;
                Map<String, String> payload = new HashMap<>();
                payload.put("collisionKey", collisionKey);
                payload.put("distance", String.format(Locale.ROOT, "%.5f", distance));

                drafts.add(new EventDraft(
                        EventType.HUMANS_COLLIDED,
                        List.of(left.getId(), right.getId()),
                        payload,
                        35,
                        collisionKey
                ));
            }
        }
        return drafts;
    }

    private List<EventDraft> buildDiscoveryDrafts(long runSeed, long tick, List<Human> humans, java.util.Set<Long> excludedHumanIds) {
        List<EventDraft> drafts = new ArrayList<>();
        for (Human human : humans) {
            if (excludedHumanIds.contains(human.getId())) {
                continue;
            }
            Random discoveryRandom = new Random(deriveDeterministicSeed(runSeed, tick, human.getId()) ^ DISCOVERY_SEED_SALT);
            if (discoveryRandom.nextInt(12) != 0) {
                continue;
            }

            int topicIndex = discoveryRandom.nextInt(INVENTION_TOPICS.length);
            String inventionKey = "INV-" + topicIndex;
            InventionCategory category = resolveFallbackDiscoveryCategory(human);
            String discoveryKey = "DISC-" + human.getId() + "-" + tick + "-" + topicIndex;
            int impactScore = 20 + discoveryRandom.nextInt(81);
            String title = INVENTION_TOPICS[topicIndex];
            String summary = "Tick " + tick + ": human " + human.getId() + " unlocked " + title.toLowerCase() + ".";

            Map<String, String> payload = new HashMap<>();
            payload.put("discoveryKey", discoveryKey);
            payload.put("inventionKey", inventionKey);
            payload.put("inventionCategory", category.name());
            payload.put("title", title);
            payload.put("summary", summary);
            payload.put("impactScore", String.valueOf(impactScore));
            payload.put("trigger", "FALLBACK_PROFILE");

            drafts.add(new EventDraft(
                    EventType.DISCOVERY_UNLOCKED,
                    List.of(human.getId()),
                    payload,
                    impactScore,
                    discoveryKey
            ));
        }
        return drafts;
    }

    private java.util.Set<Long> humansWithContextDiscovery(List<EventDraft> contextDiscoveries) {
        java.util.Set<Long> humanIds = new java.util.HashSet<>();
        for (EventDraft draft : contextDiscoveries) {
            humanIds.addAll(draft.actorIds());
        }
        return humanIds;
    }

    private List<EventDraft> buildDialogueDrafts(Long cityId, long tick, List<Human> humans) {
        List<EventDraft> drafts = new ArrayList<>();
        for (int i = 0; i < humans.size(); i++) {
            Human left = humans.get(i);
            for (int j = i + 1; j < humans.size(); j++) {
                Human right = humans.get(j);
                if (distance(left, right) > COLLISION_DISTANCE_THRESHOLD) {
                    continue;
                }
                if (left.isBusy() || right.isBusy()) {
                    continue;
                }
                long actorA = Math.min(left.getId(), right.getId());
                long actorB = Math.max(left.getId(), right.getId());
                if (hasRecentDialogueForPair(cityId, tick, actorA, actorB)) {
                    continue;
                }

                String dialogueKey = "DIALOGUE_EXCHANGED:" + actorA + ":" + actorB + ":" + tick;
                Map<String, String> payload = new HashMap<>();
                payload.put("dialogueKey", dialogueKey);
                payload.put("pair", actorA + "-" + actorB);

                drafts.add(new EventDraft(
                        EventType.DIALOGUE_EXCHANGED,
                        List.of(actorA, actorB),
                        payload,
                        30,
                        dialogueKey
                ));
            }
        }
        return drafts;
    }

    private boolean hasRecentDialogueForPair(Long cityId, long tick, long actorA, long actorB) {
        long fromTick = Math.max(0L, tick - RECENT_DIALOGUE_WINDOW_TICKS);
        return eventApplicationService.listCityEventsByType(cityId, EventType.DIALOGUE_EXCHANGED).stream()
                .filter(event -> event.getTick() >= fromTick && event.getTick() < tick)
                .anyMatch(event -> matchesPair(event.getActorIds(), actorA, actorB));
    }

    private boolean matchesPair(List<Long> actorIds, long actorA, long actorB) {
        if (actorIds == null || actorIds.size() != 2) {
            return false;
        }
        long first = Math.min(actorIds.get(0), actorIds.get(1));
        long second = Math.max(actorIds.get(0), actorIds.get(1));
        return first == actorA && second == actorB;
    }

    private List<EventDraft> buildCollisionDiscoveryDrafts(Long cityId, long tick, List<Human> humans) {
        List<EventDraft> drafts = new ArrayList<>();
        for (int i = 0; i < humans.size(); i++) {
            Human left = humans.get(i);
            for (int j = i + 1; j < humans.size(); j++) {
                Human right = humans.get(j);
                if (distance(left, right) > COLLISION_DISTANCE_THRESHOLD) {
                    continue;
                }
                Optional<InventionCategory> category = resolveCollisionDiscoveryCategory(left, right);
                if (category.isEmpty()) {
                    continue;
                }

                long actorA = Math.min(left.getId(), right.getId());
                long actorB = Math.max(left.getId(), right.getId());
                if (hasRecentCollisionDiscovery(cityId, tick, actorA, actorB)) {
                    continue;
                }

                String discoveryKey = "COLLISION_DISC:" + actorA + ":" + actorB + ":" + tick;
                Map<String, String> payload = buildCollisionDiscoveryPayload(category.get(), actorA, actorB, tick);
                drafts.add(new EventDraft(
                        EventType.DISCOVERY_UNLOCKED,
                        List.of(actorA, actorB),
                        payload,
                        discoveryImpact(actorA + actorB, tick, 37),
                        discoveryKey
                ));
            }
        }
        return drafts;
    }

    private boolean hasRecentCollisionDiscovery(Long cityId, long tick, long actorA, long actorB) {
        long fromTick = Math.max(0L, tick - RECENT_COLLISION_DISCOVERY_WINDOW_TICKS);
        return eventApplicationService.listCityEventsByType(cityId, EventType.DISCOVERY_UNLOCKED).stream()
                .filter(event -> event.getTick() >= fromTick && event.getTick() < tick)
                .filter(event -> event.getPayload() != null)
                .filter(event -> "COLLISION_COMPLEMENTARY_TRAITS".equals(event.getPayload().get("trigger")))
                .anyMatch(event -> matchesPair(event.getActorIds(), actorA, actorB));
    }

    private Map<String, String> buildCollisionDiscoveryPayload(
            InventionCategory category,
            long actorA,
            long actorB,
            long tick
    ) {
        String inventionKey = "DISCOVERY:collision:" + actorA + ":" + actorB + ":" + tick;
        String title = "Complementary traits insight";
        String summary = "Tick " + tick + ": humans " + actorA + " and " + actorB + " unlocked complementary traits insight.";
        Map<String, String> payload = new HashMap<>();
        payload.put("discoveryKey", inventionKey);
        payload.put("inventionKey", inventionKey);
        payload.put("inventionCategory", category.name());
        payload.put("title", title);
        payload.put("summary", summary);
        payload.put("impactScore", String.valueOf(discoveryImpact(actorA + actorB, tick, 41)));
        payload.put("trigger", "COLLISION_COMPLEMENTARY_TRAITS");
        return payload;
    }

    private Optional<InventionCategory> resolveCollisionDiscoveryCategory(Human left, Human right) {
        Trait dominantLeft = dominantTrait(left);
        Trait dominantRight = dominantTrait(right);
        if (dominantLeft == dominantRight) {
            return Optional.empty();
        }
        if (dominantLeft == Trait.SOCIABILITY || dominantRight == Trait.SOCIABILITY) {
            return Optional.of(InventionCategory.SOCIAL_PRACTICE);
        }
        if ((dominantLeft == Trait.CREATIVITY && dominantRight == Trait.PRACTICALITY)
                || (dominantLeft == Trait.PRACTICALITY && dominantRight == Trait.CREATIVITY)
                || (dominantLeft == Trait.PRACTICALITY && dominantRight == Trait.INTELLECT)
                || (dominantLeft == Trait.INTELLECT && dominantRight == Trait.PRACTICALITY)) {
            return Optional.of(InventionCategory.TECHNIQUE);
        }
        return Optional.of(InventionCategory.KNOWLEDGE);
    }

    private InventionCategory resolveFallbackDiscoveryCategory(Human human) {
        Optional<SimulationPlaceRegistry.SimulationPlace> place =
                resolvePlaceForPosition(human.getX(), human.getY());
        if (place.isPresent()) {
            return place.get().category();
        }
        return traitToCategory(dominantTrait(human));
    }

    private Trait dominantTrait(Human human) {
        double creativity = normalizedTrait(human.getCreativity());
        double intellect = normalizedTrait(human.getIntellect());
        double sociability = normalizedTrait(human.getSociability());
        double practicality = normalizedTrait(human.getPracticality());

        Trait dominant = Trait.CREATIVITY;
        double maxValue = creativity;
        if (intellect > maxValue) {
            dominant = Trait.INTELLECT;
            maxValue = intellect;
        }
        if (sociability > maxValue) {
            dominant = Trait.SOCIABILITY;
            maxValue = sociability;
        }
        if (practicality > maxValue) {
            dominant = Trait.PRACTICALITY;
        }
        return dominant;
    }

    private InventionCategory traitToCategory(Trait trait) {
        return switch (trait) {
            case SOCIABILITY -> InventionCategory.SOCIAL_PRACTICE;
            case PRACTICALITY, CREATIVITY -> InventionCategory.TECHNIQUE;
            case INTELLECT -> InventionCategory.KNOWLEDGE;
        };
    }

    private double normalizedTrait(Double value) {
        if (value == null || !Double.isFinite(value)) {
            return 0.5;
        }
        return Math.max(0D, Math.min(1D, value));
    }

    private List<EventDraft> buildReachedPlaceDrafts(
            Long cityId,
            long tick,
            List<Human> humans,
            Map<Long, String> previousPlaceByHuman
    ) {
        List<EventDraft> drafts = new ArrayList<>();
        for (Human human : humans) {
            if (human.isBusy()) {
                continue;
            }
            Optional<SimulationPlaceRegistry.SimulationPlace> currentPlace =
                    resolvePlaceForPosition(human.getX(), human.getY());
            if (currentPlace.isEmpty()) {
                continue;
            }
            String previousPlaceId = previousPlaceByHuman.get(human.getId());
            if (currentPlace.get().id().equals(previousPlaceId)) {
                continue;
            }
            if (hasRecentReachedPlaceDiscovery(cityId, human.getId(), currentPlace.get().id(), tick)) {
                continue;
            }

            String discoveryKey = "REACHED:" + human.getId() + ":" + currentPlace.get().id() + ":" + tick;
            Map<String, String> payload = buildDiscoveryPayload(
                    currentPlace.get().category(),
                    currentPlace.get().id(),
                    human.getId(),
                    tick,
                    "Reached " + currentPlace.get().id(),
                    "REACHED_PLACE"
            );

            drafts.add(new EventDraft(
                    EventType.DISCOVERY_UNLOCKED,
                    List.of(human.getId()),
                    payload,
                    discoveryImpact(human.getId(), tick, 29),
                    discoveryKey
            ));
        }
        return drafts;
    }

    private List<EventDraft> buildStayedAtPlaceDrafts(Long cityId, long tick, List<Human> humans) {
        List<EventDraft> drafts = new ArrayList<>();
        for (Human human : humans) {
            if (human.isBusy()) {
                continue;
            }
            Optional<SimulationPlaceRegistry.SimulationPlace> currentPlace =
                    resolvePlaceForPosition(human.getX(), human.getY());
            String stateKey = placeStateKey(cityId, human.getId());
            if (currentPlace.isEmpty()) {
                lastPlaceByHuman.remove(stateKey);
                placeStreakByHuman.remove(stateKey);
                continue;
            }

            String currentPlaceId = currentPlace.get().id();
            String previousPlaceId = lastPlaceByHuman.get(stateKey);
            int nextStreak = currentPlaceId.equals(previousPlaceId)
                    ? placeStreakByHuman.getOrDefault(stateKey, 0) + 1
                    : 1;
            lastPlaceByHuman.put(stateKey, currentPlaceId);
            placeStreakByHuman.put(stateKey, nextStreak);

            if (nextStreak < STAYED_AT_PLACE_THRESHOLD_TICKS) {
                continue;
            }
            if (hasRecentStayedPlaceDiscovery(cityId, human.getId(), currentPlaceId, tick)) {
                continue;
            }

            String discoveryKey = "STAYED:" + human.getId() + ":" + currentPlaceId + ":" + tick;
            Map<String, String> payload = buildDiscoveryPayload(
                    currentPlace.get().category(),
                    currentPlaceId,
                    human.getId(),
                    tick,
                    "Stayed at " + currentPlaceId,
                    "STAYED_AT_PLACE"
            );
            drafts.add(new EventDraft(
                    EventType.DISCOVERY_UNLOCKED,
                    List.of(human.getId()),
                    payload,
                    discoveryImpact(human.getId(), tick, 33),
                    discoveryKey
            ));
        }
        return drafts;
    }

    private List<EventDraft> buildProximityGroupDrafts(Long cityId, long tick, List<Human> humans) {
        List<EventDraft> drafts = new ArrayList<>();
        List<List<Human>> groups = buildProximityGroups(humans);
        java.util.Set<String> previousGroups = previousProximityGroupsByCity
                .getOrDefault(cityId, java.util.Collections.emptySet());
        java.util.Set<String> currentGroups = new java.util.HashSet<>();

        for (List<Human> group : groups) {
            String groupKey = groupSignature(group);
            currentGroups.add(groupKey);
            String streakKey = proximityGroupStateKey(cityId, groupKey);
            int streak = previousGroups.contains(groupKey)
                    ? proximityGroupStreakByKey.getOrDefault(streakKey, 0) + 1
                    : 1;
            proximityGroupStreakByKey.put(streakKey, streak);
            if (streak < PROXIMITY_GROUP_SUSTAIN_TICKS) {
                continue;
            }
            if (hasRecentProximityGroupEvent(cityId, tick, groupKey)) {
                continue;
            }

            if (group.size() >= 3) {
                List<Long> actorIds = group.stream().map(Human::getId).toList();
                String discoveryKey = "PROX_GROUP_DISC:" + groupKey + ":" + tick;
                drafts.add(new EventDraft(
                        EventType.DISCOVERY_UNLOCKED,
                        actorIds,
                        buildProximityGroupDiscoveryPayload(groupKey, actorIds, tick),
                        discoveryImpact(actorIds.stream().mapToLong(Long::longValue).sum(), tick, 39),
                        discoveryKey
                ));
            } else {
                long actorA = Math.min(group.get(0).getId(), group.get(1).getId());
                long actorB = Math.max(group.get(0).getId(), group.get(1).getId());
                String dialogueKey = "PROX_GROUP_DIALOGUE:" + actorA + ":" + actorB + ":" + tick;
                Map<String, String> payload = new HashMap<>();
                payload.put("dialogueKey", dialogueKey);
                payload.put("pair", actorA + "-" + actorB);
                payload.put("groupKey", groupKey);
                payload.put("trigger", "PROXIMITY_GROUP");
                drafts.add(new EventDraft(
                        EventType.DIALOGUE_EXCHANGED,
                        List.of(actorA, actorB),
                        payload,
                        28,
                        dialogueKey
                ));
            }
        }

        previousProximityGroupsByCity.put(cityId, currentGroups);
        String prefix = cityId + ":";
        proximityGroupStreakByKey.keySet().removeIf(key -> key.startsWith(prefix) && !currentGroups.contains(key.substring(prefix.length())));
        return drafts;
    }

    private List<List<Human>> buildProximityGroups(List<Human> humans) {
        int size = humans.size();
        List<List<Integer>> adjacency = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            adjacency.add(new ArrayList<>());
        }
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                if (distance(humans.get(i), humans.get(j)) <= PROXIMITY_GROUP_DISTANCE_THRESHOLD) {
                    adjacency.get(i).add(j);
                    adjacency.get(j).add(i);
                }
            }
        }

        boolean[] visited = new boolean[size];
        List<List<Human>> groups = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (visited[i]) {
                continue;
            }
            List<Human> component = new ArrayList<>();
            ArrayList<Integer> stack = new ArrayList<>();
            stack.add(i);
            visited[i] = true;
            while (!stack.isEmpty()) {
                int node = stack.remove(stack.size() - 1);
                component.add(humans.get(node));
                for (int neighbor : adjacency.get(node)) {
                    if (!visited[neighbor]) {
                        visited[neighbor] = true;
                        stack.add(neighbor);
                    }
                }
            }
            if (component.size() >= 2) {
                component.sort(Comparator.comparing(Human::getId));
                groups.add(component);
            }
        }
        return groups;
    }

    private String groupSignature(List<Human> group) {
        return group.stream()
                .map(Human::getId)
                .sorted()
                .map(String::valueOf)
                .reduce((left, right) -> left + "-" + right)
                .orElse("");
    }

    private boolean hasRecentProximityGroupEvent(Long cityId, long tick, String groupKey) {
        long blockedFromTick = Math.max(0L, tick - PROXIMITY_GROUP_COOLDOWN_TICKS + 1L);
        boolean discoveryRecentlyEmitted = eventApplicationService.listCityEventsByType(cityId, EventType.DISCOVERY_UNLOCKED).stream()
                .filter(event -> event.getTick() >= blockedFromTick && event.getTick() < tick)
                .filter(event -> event.getPayload() != null)
                .anyMatch(event -> "PROXIMITY_GROUP".equals(event.getPayload().get("trigger"))
                        && groupKey.equals(event.getPayload().get("groupKey")));
        if (discoveryRecentlyEmitted) {
            return true;
        }

        return eventApplicationService.listCityEventsByType(cityId, EventType.DIALOGUE_EXCHANGED).stream()
                .filter(event -> event.getTick() >= blockedFromTick && event.getTick() < tick)
                .filter(event -> event.getPayload() != null)
                .anyMatch(event -> "PROXIMITY_GROUP".equals(event.getPayload().get("trigger"))
                        && groupKey.equals(event.getPayload().get("groupKey")));
    }

    private Map<String, String> buildProximityGroupDiscoveryPayload(String groupKey, List<Long> actorIds, long tick) {
        String inventionKey = "DISCOVERY:proximity-group:" + groupKey + ":" + tick;
        String title = "Group social practice insight";
        String summary = "Tick " + tick + ": group " + actorIds + " unlocked social practice insight.";
        Map<String, String> payload = new HashMap<>();
        payload.put("discoveryKey", inventionKey);
        payload.put("inventionKey", inventionKey);
        payload.put("inventionCategory", InventionCategory.SOCIAL_PRACTICE.name());
        payload.put("title", title);
        payload.put("summary", summary);
        payload.put("impactScore", String.valueOf(discoveryImpact(actorIds.stream().mapToLong(Long::longValue).sum(), tick, 43)));
        payload.put("trigger", "PROXIMITY_GROUP");
        payload.put("groupKey", groupKey);
        return payload;
    }

    private boolean hasRecentReachedPlaceDiscovery(Long cityId, Long humanId, String placeId, long tick) {
        long blockedFromTick = Math.max(0L, tick - REACHED_PLACE_COOLDOWN_TICKS + 1L);
        return eventApplicationService.listCityEventsByType(cityId, EventType.DISCOVERY_UNLOCKED).stream()
                .filter(event -> event.getTick() >= blockedFromTick && event.getTick() < tick)
                .anyMatch(event -> isReachedPlaceDiscovery(event, humanId, placeId));
    }

    private boolean hasRecentStayedPlaceDiscovery(Long cityId, Long humanId, String placeId, long tick) {
        long blockedFromTick = Math.max(0L, tick - STAYED_AT_PLACE_COOLDOWN_TICKS + 1L);
        return eventApplicationService.listCityEventsByType(cityId, EventType.DISCOVERY_UNLOCKED).stream()
                .filter(event -> event.getTick() >= blockedFromTick && event.getTick() < tick)
                .anyMatch(event -> isStayedPlaceDiscovery(event, humanId, placeId));
    }

    private boolean isReachedPlaceDiscovery(Event event, Long humanId, String placeId) {
        if (event.getActorIds() == null || !event.getActorIds().contains(humanId)) {
            return false;
        }
        if (event.getPayload() == null) {
            return false;
        }
        String trigger = event.getPayload().getOrDefault("trigger", "");
        String payloadPlaceId = event.getPayload().getOrDefault("placeId", "");
        return "REACHED_PLACE".equals(trigger) && placeId.equals(payloadPlaceId);
    }

    private boolean isStayedPlaceDiscovery(Event event, Long humanId, String placeId) {
        if (event.getActorIds() == null || !event.getActorIds().contains(humanId)) {
            return false;
        }
        if (event.getPayload() == null) {
            return false;
        }
        String trigger = event.getPayload().getOrDefault("trigger", "");
        String payloadPlaceId = event.getPayload().getOrDefault("placeId", "");
        return "STAYED_AT_PLACE".equals(trigger) && placeId.equals(payloadPlaceId);
    }

    private Map<String, String> buildDiscoveryPayload(
            InventionCategory category,
            String contextKey,
            Long humanId,
            long tick,
            String titlePrefix,
            String trigger
    ) {
        String inventionKey = "DISCOVERY:" + contextKey + ":" + humanId + ":" + tick;
        String title = titlePrefix + " insight";
        String summary = "Tick " + tick + ": human " + humanId + " unlocked " + title.toLowerCase(Locale.ROOT) + ".";
        Map<String, String> payload = new HashMap<>();
        payload.put("discoveryKey", inventionKey);
        payload.put("inventionKey", inventionKey);
        payload.put("inventionCategory", category.name());
        payload.put("title", title);
        payload.put("summary", summary);
        payload.put("impactScore", String.valueOf(discoveryImpact(humanId, tick, 31)));
        payload.put("placeId", contextKey);
        payload.put("trigger", trigger);
        return payload;
    }

    private int discoveryImpact(Long humanId, long tick, int spread) {
        return 30 + (int) Math.floorMod((humanId * 17L) + tick, spread);
    }

    private Optional<SimulationPlaceRegistry.SimulationPlace> resolvePlaceForPosition(Double x, Double y) {
        if (x == null || y == null || !Double.isFinite(x) || !Double.isFinite(y)) {
            return Optional.empty();
        }
        return SimulationPlaceRegistry.all().stream()
                .filter(place -> distanceTo(x, y, place.x(), place.y()) <= place.radius())
                .min(Comparator.comparingDouble(place -> distanceTo(x, y, place.x(), place.y())));
    }

    private double distanceTo(double xA, double yA, double xB, double yB) {
        double deltaX = xA - xB;
        double deltaY = yA - yB;
        return Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
    }

    private void emitMilestoneEvents(Long cityId, List<Invention> inventions) {
        if (inventions == null || inventions.isEmpty()) {
            return;
        }

        Map<Long, List<EventDraft>> draftsByTick = new HashMap<>();
        for (Invention invention : inventions) {
            Map<String, String> payload = new HashMap<>();
            payload.put("inventionKey", invention.getInventionKey());
            payload.put("inventionCategory", invention.getCategory().name());
            payload.put("title", invention.getTitle());
            payload.put("sourceEventKey", invention.getSourceEventKeys().isEmpty() ? "" : invention.getSourceEventKeys().get(0));

            EventDraft draft = new EventDraft(
                    EventType.INVENTION_EMERGED,
                    List.of(),
                    payload,
                    invention.getImpactScore(),
                    invention.getInventionKey()
            );
            draftsByTick.computeIfAbsent(invention.getTickCreated(), ignored -> new ArrayList<>()).add(draft);
        }

        draftsByTick.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> eventApplicationService.emitEventsAtTick(
                        cityId,
                        entry.getKey(),
                        entry.getValue()
                ));
    }

    private double distance(Human left, Human right) {
        double deltaX = left.getX() - right.getX();
        double deltaY = left.getY() - right.getY();
        return Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
    }

    private String placeStateKey(Long cityId, Long humanId) {
        return cityId + ":" + humanId;
    }

    private void resetPlaceStayState(Long cityId) {
        String prefix = cityId + ":";
        lastPlaceByHuman.keySet().removeIf(key -> key.startsWith(prefix));
        placeStreakByHuman.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private String proximityGroupStateKey(Long cityId, String groupKey) {
        return cityId + ":" + groupKey;
    }

    private void resetProximityGroupState(Long cityId) {
        previousProximityGroupsByCity.remove(cityId);
        String prefix = cityId + ":";
        proximityGroupStreakByKey.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public record TimelineHistory(
            Long cityId,
            Long fromTick,
            Long toTick,
            List<Event> events,
            List<Invention> inventions
    ) {
    }

    private enum Trait {
        CREATIVITY,
        INTELLECT,
        SOCIABILITY,
        PRACTICALITY
    }
}
