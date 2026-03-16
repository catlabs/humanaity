package eu.catlabs.humanaity.simulation.application;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.domain.EventType;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.invention.infrastructure.persistence.InventionRepository;
import eu.catlabs.humanaity.simulation.domain.HumanGoal;
import eu.catlabs.humanaity.simulation.domain.HumanGoalSource;
import eu.catlabs.humanaity.simulation.domain.HumanGoalStatus;
import eu.catlabs.humanaity.simulation.domain.HumanGoalType;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.HumanGoalRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.KnowledgeUnlockRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.SimulationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:goal-exec;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
class SimulationGoalExecutionTest {

    @Autowired
    private SimulationApplicationService simulationApplicationService;
    @Autowired
    private HumanGoalApplicationService humanGoalApplicationService;
    @Autowired
    private HumanGoalRepository humanGoalRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private InventionRepository inventionRepository;
    @Autowired
    private HumanRepository humanRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private SimulationRunRepository simulationRunRepository;
    @Autowired
    private KnowledgeUnlockRepository knowledgeUnlockRepository;

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
    void moveToPlaceGoalCompletesDeterministicallyAndRecordsLifecycleEvent() {
        City city = createCity("Goals-Move");
        Human human = createHuman(city, "Elsa", 0.90, 0.90);
        simulationApplicationService.createRun(city.getId(), 90210L);

        HumanGoal assigned = humanGoalApplicationService.assignGoal(
                city.getId(),
                human.getId(),
                HumanGoalType.MOVE_TO_PLACE,
                HumanGoalSource.CHAT_COMMAND,
                0L,
                new HumanGoalApplicationService.GoalTarget("forest", null, 0.14, 0.18, "test-move")
        );

        simulationApplicationService.step(city.getId(), 80);

        Optional<HumanGoal> activeGoal = humanGoalApplicationService.findActiveGoal(human.getId());
        HumanGoal persistedGoal = humanGoalRepository.findById(assigned.getId()).orElseThrow();
        Human updatedHuman = humanRepository.findById(human.getId()).orElseThrow();
        List<Event> completionEvents = eventRepository.findByCityIdAndEventTypeOrderByTickAscSequenceInTickAscIdAsc(
                city.getId(),
                EventType.GOAL_COMPLETED
        );

        assertThat(activeGoal).isEmpty();
        assertThat(persistedGoal.getStatus()).isEqualTo(HumanGoalStatus.COMPLETED);
        assertThat(persistedGoal.getCompletedTick()).isNotNull();
        assertThat(updatedHuman.getX()).isBetween(0.0, 1.0);
        assertThat(updatedHuman.getY()).isBetween(0.0, 1.0);
        assertThat(completionEvents).hasSize(1);
        assertThat(completionEvents.get(0).getPayload().get("goalId")).isEqualTo(String.valueOf(assigned.getId()));
        assertThat(completionEvents.get(0).getPayload().get("goalType")).isEqualTo(HumanGoalType.MOVE_TO_PLACE.name());
    }

    @Test
    void sameSeedAndGoalStateYieldSameMeetGoalResult() {
        long seed = 202624L;
        City city = createCity("Goals-Meet-Determinism");
        Human actor = createHuman(city, "Pierre", 0.20, 0.20);
        Human target = createHuman(city, "Lucas", 0.82, 0.82);
        List<HumanSeedState> initialState = humanRepository.findByCityIdOrderByIdAsc(city.getId()).stream()
                .map(human -> new HumanSeedState(human.getId(), human.getX(), human.getY()))
                .toList();

        CanonicalMeetResult first = runMeetGoalScenario(city.getId(), actor.getId(), target.getId(), seed);

        humanGoalRepository.deleteAll();
        inventionRepository.deleteAll();
        eventRepository.deleteAll();
        simulationRunRepository.deleteAll();
        restoreHumans(city.getId(), initialState);

        CanonicalMeetResult second = runMeetGoalScenario(city.getId(), actor.getId(), target.getId(), seed);

        assertThat(second).isEqualTo(first);
    }

    private City createCity(String name) {
        City city = new City();
        city.setName(name);
        return cityRepository.save(city);
    }

    private Human createHuman(City city, String name, double x, double y) {
        Human human = new Human();
        human.setCity(city);
        human.setName(name);
        human.setBusy(false);
        human.setX(x);
        human.setY(y);
        return humanRepository.save(human);
    }

    private CanonicalMeetResult runMeetGoalScenario(Long cityId, Long actorId, Long targetId, long seed) {
        simulationApplicationService.createRun(cityId, seed);
        humanGoalApplicationService.assignGoal(
                cityId,
                actorId,
                HumanGoalType.MEET_HUMAN,
                HumanGoalSource.AUTONOMOUS,
                0L,
                new HumanGoalApplicationService.GoalTarget(null, targetId, null, null, "meet-determinism")
        );
        simulationApplicationService.step(cityId, 60);

        Human finalActor = humanRepository.findById(actorId).orElseThrow();
        List<Event> completionEvents = eventRepository.findByCityIdAndEventTypeOrderByTickAscSequenceInTickAscIdAsc(
                cityId,
                EventType.GOAL_COMPLETED
        );
        return new CanonicalMeetResult(
                finalActor.getX(),
                finalActor.getY(),
                completionEvents.stream().map(Event::getTick).toList()
        );
    }

    private void restoreHumans(Long cityId, List<HumanSeedState> initialState) {
        Map<Long, HumanSeedState> stateById = initialState.stream()
                .collect(java.util.stream.Collectors.toMap(HumanSeedState::id, state -> state));
        List<Human> humans = humanRepository.findByCityIdOrderByIdAsc(cityId);
        for (Human human : humans) {
            HumanSeedState state = stateById.get(human.getId());
            human.setBusy(false);
            human.setX(state.x());
            human.setY(state.y());
        }
        humanRepository.saveAll(humans);
    }

    private record HumanSeedState(Long id, Double x, Double y) {
    }

    private record CanonicalMeetResult(Double x, Double y, List<Long> completionTicks) {
    }
}
