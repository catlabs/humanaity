package eu.catlabs.humanaity.simulation.application;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.domain.EventType;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.invention.application.InventionApplicationService;
import eu.catlabs.humanaity.invention.domain.Invention;
import eu.catlabs.humanaity.invention.infrastructure.persistence.InventionRepository;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.SimulationRunRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.HumanGoalRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.KnowledgeUnlockRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeHouseRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeKnownPlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:history-repro;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
class SimulationHistoryReproducibilityTest {

    @Autowired
    private SimulationApplicationService simulationApplicationService;
    @Autowired
    private InventionApplicationService inventionApplicationService;
    @Autowired
    private InventionRepository inventionRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private HumanRepository humanRepository;
    @Autowired
    private SimulationRunRepository simulationRunRepository;
    @Autowired
    private HumanGoalRepository humanGoalRepository;
    @Autowired
    private KnowledgeUnlockRepository knowledgeUnlockRepository;
    @Autowired
    private TribeHouseRepository tribeHouseRepository;
    @Autowired
    private TribeKnownPlaceRepository tribeKnownPlaceRepository;

    @BeforeEach
    void cleanDatabase() {
        tribeKnownPlaceRepository.deleteAll();
        tribeHouseRepository.deleteAll();
        knowledgeUnlockRepository.deleteAll();
        humanGoalRepository.deleteAll();
        inventionRepository.deleteAll();
        eventRepository.deleteAll();
        simulationRunRepository.deleteAll();
        humanRepository.deleteAll();
        cityRepository.deleteAll();
    }

    @Test
    void sameSeedAndStepSequenceYieldEquivalentOrderedEventsAndInventions() {
        long seed = 20260313L;
        int steps = 120;
        City city = createCityWithHumans("Alpha");
        List<HumanSeedState> initialState = humanRepository.findByCityIdOrderByIdAsc(city.getId()).stream()
                .map(human -> new HumanSeedState(
                        human.getId(),
                        human.getX(),
                        human.getY(),
                        human.isBusy(),
                        human.getNextGoalAssignTick()
                ))
                .toList();

        simulationApplicationService.createRun(city.getId(), seed);
        simulationApplicationService.step(city.getId(), steps);

        List<CanonicalEvent> firstEvents = simulationApplicationService
                .listCityEvents(city.getId(), 0L, null, 1_000)
                .stream()
                .map(this::toCanonicalEvent)
                .toList();
        List<CanonicalInvention> firstInventions = simulationApplicationService
                .listCityInventions(city.getId(), 0L, null, 1_000)
                .stream()
                .map(this::toCanonicalInvention)
                .toList();
        assertThat(firstEvents.stream())
                .noneMatch(event -> "FALLBACK_PROFILE".equals(event.payload().get("trigger")));

        humanGoalRepository.deleteAll();
        inventionRepository.deleteAll();
        eventRepository.deleteAll();
        simulationRunRepository.deleteAll();
        restoreHumans(city.getId(), initialState);

        simulationApplicationService.createRun(city.getId(), seed);
        simulationApplicationService.step(city.getId(), steps);

        List<CanonicalEvent> secondEvents = simulationApplicationService
                .listCityEvents(city.getId(), 0L, null, 1_000)
                .stream()
                .map(this::toCanonicalEvent)
                .toList();
        List<CanonicalInvention> secondInventions = simulationApplicationService
                .listCityInventions(city.getId(), 0L, null, 1_000)
                .stream()
                .map(this::toCanonicalInvention)
                .toList();
        assertThat(secondEvents.stream())
                .noneMatch(event -> "FALLBACK_PROFILE".equals(event.payload().get("trigger")));

        assertThat(firstEvents).isNotEmpty();
        assertThat(firstInventions).isNotEmpty();
        assertThat(secondEvents).isEqualTo(firstEvents);
        assertThat(secondInventions).isEqualTo(firstInventions);
    }

    @Test
    void inventionDerivationIsIdempotentForEquivalentPersistedDiscoveryPatterns() {
        City city = createCityWithHumans("Gamma");
        simulationApplicationService.createRun(city.getId(), 777L);
        simulationApplicationService.step(city.getId(), 80);

        int countBefore = simulationApplicationService.listCityInventions(city.getId(), 0L, null, 1_000).size();
        List<Invention> firstReplay = inventionApplicationService.deriveFromPersistedEvents(city.getId());
        List<Invention> secondReplay = inventionApplicationService.deriveFromPersistedEvents(city.getId());
        int countAfter = simulationApplicationService.listCityInventions(city.getId(), 0L, null, 1_000).size();

        assertThat(firstReplay).isEmpty();
        assertThat(secondReplay).isEmpty();
        assertThat(countAfter).isEqualTo(countBefore);
    }

    private City createCityWithHumans(String name) {
        City city = new City();
        city.setName(name);
        City savedCity = cityRepository.save(city);

        humanRepository.save(createHuman(savedCity, "Ada", 0.40, 0.40));
        humanRepository.save(createHuman(savedCity, "Bea", 0.43, 0.41));
        humanRepository.save(createHuman(savedCity, "Cai", 0.45, 0.39));
        return savedCity;
    }

    private Human createHuman(City city, String name, double x, double y) {
        Human human = new Human();
        human.setCity(city);
        human.setName(name);
        human.setBusy(false);
        human.setX(x);
        human.setY(y);
        return human;
    }

    private CanonicalEvent toCanonicalEvent(Event event) {
        return new CanonicalEvent(
                event.getEventCategory().name(),
                event.getEventType().name(),
                normalizeText(event.getEventKey()),
                actorCount(event.getActorIds()),
                event.getImportance(),
                stablePayload(event.getPayload())
        );
    }

    private Integer actorCount(List<Long> actorIds) {
        return actorIds == null ? 0 : actorIds.size();
    }

    private Map<String, String> stablePayload(Map<String, String> payload) {
        Map<String, String> normalized = new java.util.LinkedHashMap<>(payload);
        normalized.replaceAll((key, value) -> normalizeText(value));
        return normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\d+", "<n>");
    }

    private CanonicalInvention toCanonicalInvention(Invention invention) {
        return new CanonicalInvention(
                invention.getTickCreated(),
                invention.getYearCreated(),
                invention.getEraCreated().name(),
                invention.getCategory().name(),
                invention.getInventionKey(),
                invention.getTitle(),
                invention.getSummary(),
                invention.getSourceEventKeys(),
                invention.getImpactScore()
        );
    }

    private void restoreHumans(Long cityId, List<HumanSeedState> initialState) {
        Map<Long, HumanSeedState> stateById = initialState.stream()
                .collect(java.util.stream.Collectors.toMap(HumanSeedState::id, state -> state));

        List<Human> humans = humanRepository.findByCityIdOrderByIdAsc(cityId);
        for (Human human : humans) {
            HumanSeedState seedState = stateById.get(human.getId());
            human.setBusy(seedState.busy());
            human.setX(seedState.x());
            human.setY(seedState.y());
            human.setNextGoalAssignTick(seedState.nextGoalAssignTick());
        }
        humanRepository.saveAll(humans);
    }

    private record CanonicalEvent(
            String eventCategory,
            String eventType,
            String eventKey,
            Integer actorCount,
            Integer importance,
            Map<String, String> payload
    ) {
    }

    private record CanonicalInvention(
            Long tickCreated,
            Integer yearCreated,
            String eraCreated,
            String category,
            String inventionKey,
            String title,
            String summary,
            List<String> sourceEventKeys,
            Integer impactScore
    ) {
    }

    private record HumanSeedState(Long id, Double x, Double y, boolean busy, Long nextGoalAssignTick) {
    }
}
