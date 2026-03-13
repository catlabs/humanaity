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
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<Long, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();
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
        List<Human> orderedHumans = new ArrayList<>(humanRepository.findByCityIdOrderByIdAsc(run.getCity().getId()));
        if (orderedHumans.isEmpty()) {
            logger.debug("No humans found in city {}", run.getCity().getId());
            long nextTick = run.getTick() + 1;
            run.setTick(nextTick);
            simulationRunRepository.save(run);
            emitMilestoneEvents(run.getCity().getId(), inventionApplicationService.deriveFromPersistedEvents(run.getCity().getId()));
            return;
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

        List<EventDraft> stepEvents = buildStepEventDrafts(run, nextTick, orderedHumans);
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

    private List<EventDraft> buildStepEventDrafts(SimulationRun run, long tick, List<Human> orderedHumans) {
        List<Human> humans = orderedHumans.stream()
                .sorted(Comparator.comparing(Human::getId))
                .toList();
        List<EventDraft> drafts = new ArrayList<>();
        drafts.addAll(buildCollisionDrafts(tick, humans));
        drafts.addAll(buildDiscoveryDrafts(run.getSeed(), tick, humans));
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

    private List<EventDraft> buildDiscoveryDrafts(long runSeed, long tick, List<Human> humans) {
        List<EventDraft> drafts = new ArrayList<>();
        for (Human human : humans) {
            Random discoveryRandom = new Random(deriveDeterministicSeed(runSeed, tick, human.getId()) ^ DISCOVERY_SEED_SALT);
            if (discoveryRandom.nextInt(12) != 0) {
                continue;
            }

            int topicIndex = discoveryRandom.nextInt(INVENTION_TOPICS.length);
            String inventionKey = "INV-" + topicIndex;
            String inventionCategory = switch (topicIndex % 3) {
                case 0 -> "TECHNIQUE";
                case 1 -> "SOCIAL_PRACTICE";
                default -> "KNOWLEDGE";
            };
            String discoveryKey = "DISC-" + human.getId() + "-" + tick + "-" + topicIndex;
            int impactScore = 20 + discoveryRandom.nextInt(81);
            String title = INVENTION_TOPICS[topicIndex];
            String summary = "Tick " + tick + ": human " + human.getId() + " unlocked " + title.toLowerCase() + ".";

            Map<String, String> payload = new HashMap<>();
            payload.put("discoveryKey", discoveryKey);
            payload.put("inventionKey", inventionKey);
            payload.put("inventionCategory", inventionCategory);
            payload.put("title", title);
            payload.put("summary", summary);
            payload.put("impactScore", String.valueOf(impactScore));

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

    public record TimelineHistory(
            Long cityId,
            Long fromTick,
            Long toTick,
            List<Event> events,
            List<Invention> inventions
    ) {
    }
}
