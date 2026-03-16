package eu.catlabs.humanaity.simulation.application;

import eu.catlabs.humanaity.ai.application.AiGenerationService;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.domain.EventType;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.invention.infrastructure.persistence.InventionRepository;
import eu.catlabs.humanaity.simulation.domain.KnowledgeUnlock;
import eu.catlabs.humanaity.simulation.domain.TechTreeNodeType;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.HumanGoalRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.KnowledgeUnlockRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.SimulationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:autonomous-step;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
class SimulationAutonomousSteppingTest {

    @Autowired
    private SimulationApplicationService simulationApplicationService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private InventionRepository inventionRepository;
    @Autowired
    private SimulationRunRepository simulationRunRepository;
    @Autowired
    private HumanGoalRepository humanGoalRepository;
    @Autowired
    private KnowledgeUnlockRepository knowledgeUnlockRepository;
    @Autowired
    private HumanRepository humanRepository;
    @Autowired
    private CityRepository cityRepository;

    @MockBean
    private AiGenerationService aiGenerationService;

    @BeforeEach
    void cleanDatabase() {
        knowledgeUnlockRepository.deleteAll();
        humanGoalRepository.deleteAll();
        inventionRepository.deleteAll();
        eventRepository.deleteAll();
        simulationRunRepository.deleteAll();
        humanRepository.deleteAll();
        cityRepository.deleteAll();
    }

    @Test
    void autonomousStepLoopDoesNotCallAiAndKeepsUnlockedActionsDeterministic() {
        long seed = 20262430L;
        int steps = 180;
        City city = createCityWithHumans();
        List<SeedState> initialState = captureSeedState(city.getId());
        unlockCreateArt(city, 0L);

        simulationApplicationService.createRun(city.getId(), seed);
        simulationApplicationService.step(city.getId(), steps);
        List<CanonicalEvent> firstRun = canonicalize(city.getId());

        verifyNoInteractions(aiGenerationService);
        assertThat(firstRun).isNotEmpty();
        assertThat(firstRun.stream().anyMatch(event -> event.eventType() == EventType.HUMAN_ACTION_PERFORMED)).isTrue();

        knowledgeUnlockRepository.deleteAll();
        inventionRepository.deleteAll();
        eventRepository.deleteAll();
        simulationRunRepository.deleteAll();
        restoreHumans(city.getId(), initialState);
        unlockCreateArt(city, 0L);

        simulationApplicationService.createRun(city.getId(), seed);
        simulationApplicationService.step(city.getId(), steps);
        List<CanonicalEvent> secondRun = canonicalize(city.getId());

        verifyNoInteractions(aiGenerationService);
        assertThat(secondRun).isEqualTo(firstRun);
    }

    private City createCityWithHumans() {
        City city = new City();
        city.setName("Autonomous City");
        City saved = cityRepository.save(city);
        humanRepository.save(createHuman(saved, "A", "tribe-north", 0.36, 0.70));
        humanRepository.save(createHuman(saved, "B", "tribe-north", 0.38, 0.72));
        humanRepository.save(createHuman(saved, "C", "tribe-south", 0.41, 0.73));
        return saved;
    }

    private Human createHuman(City city, String name, String tribeId, double x, double y) {
        Human human = new Human();
        human.setCity(city);
        human.setName(name);
        human.setTribeId(tribeId);
        human.setBusy(false);
        human.setX(x);
        human.setY(y);
        return human;
    }

    private void unlockCreateArt(City city, long tick) {
        KnowledgeUnlock unlock = new KnowledgeUnlock();
        unlock.setCity(city);
        unlock.setNodeId("APP_CREATE_ART");
        unlock.setNodeType(TechTreeNodeType.APPLICATION);
        unlock.setUnlockedTick(tick);
        unlock.setTriggerEventType("BOOTSTRAP_TEST_UNLOCK");
        knowledgeUnlockRepository.save(unlock);
    }

    private List<SeedState> captureSeedState(Long cityId) {
        return humanRepository.findByCityIdOrderByIdAsc(cityId).stream()
                .map(human -> new SeedState(human.getId(), human.getX(), human.getY()))
                .toList();
    }

    private void restoreHumans(Long cityId, List<SeedState> state) {
        Map<Long, SeedState> stateById = state.stream()
                .collect(Collectors.toMap(SeedState::id, value -> value));
        List<Human> humans = humanRepository.findByCityIdOrderByIdAsc(cityId);
        for (Human human : humans) {
            SeedState snapshot = stateById.get(human.getId());
            human.setBusy(false);
            human.setX(snapshot.x());
            human.setY(snapshot.y());
        }
        humanRepository.saveAll(humans);
    }

    private List<CanonicalEvent> canonicalize(Long cityId) {
        return eventRepository.findByCityIdOrderByTickAscSequenceInTickAscIdAsc(cityId).stream()
                .map(event -> new CanonicalEvent(
                        event.getTick(),
                        event.getSequenceInTick(),
                        event.getEventType(),
                        event.getActorIds(),
                        event.getPayload()
                ))
                .toList();
    }

    private record SeedState(Long id, Double x, Double y) {
    }

    private record CanonicalEvent(
            Long tick,
            Integer sequenceInTick,
            EventType eventType,
            List<Long> actorIds,
            Map<String, String> payload
    ) {
    }
}
