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

        HumanGoal persistedGoal = humanGoalRepository.findById(assigned.getId()).orElseThrow();
        Human updatedHuman = humanRepository.findById(human.getId()).orElseThrow();
        List<Event> completionEvents = eventRepository.findByCityIdAndEventTypeOrderByTickAscSequenceInTickAscIdAsc(
                city.getId(),
                EventType.GOAL_COMPLETED
        );

        assertThat(persistedGoal.getStatus()).isEqualTo(HumanGoalStatus.COMPLETED);
        assertThat(persistedGoal.getCompletedTick()).isNotNull();
        assertThat(updatedHuman.getX()).isBetween(0.0, 1.0);
        assertThat(updatedHuman.getY()).isBetween(0.0, 1.0);
        assertThat(completionEvents).isNotEmpty();
        assertThat(completionEvents).anyMatch(
                event -> String.valueOf(assigned.getId()).equals(event.getPayload().get("goalId"))
        );
        assertThat(completionEvents).anyMatch(
                event -> HumanGoalType.MOVE_TO_PLACE.name().equals(event.getPayload().get("goalType"))
        );
    }

    @Test
    void completedGoalTriggersDwellThenDeterministicAutonomousReassignment() {
        City city = createCity("Goals-Dwell");
        Human human = createHuman(city, "Nora", 0.14, 0.18);
        simulationApplicationService.createRun(city.getId(), 777L);

        HumanGoal firstGoal = humanGoalApplicationService.assignGoal(
                city.getId(),
                human.getId(),
                HumanGoalType.MOVE_TO_PLACE,
                HumanGoalSource.CHAT_COMMAND,
                0L,
                new HumanGoalApplicationService.GoalTarget("forest", null, 0.14, 0.18, "test-dwell")
        );

        simulationApplicationService.step(city.getId(), 1);

        HumanGoal completedFirst = humanGoalRepository.findById(firstGoal.getId()).orElseThrow();
        Human afterCompletion = humanRepository.findById(human.getId()).orElseThrow();
        assertThat(completedFirst.getStatus()).isEqualTo(HumanGoalStatus.COMPLETED);
        assertThat(completedFirst.getCompletedTick()).isEqualTo(1L);
        assertThat(afterCompletion.getNextGoalAssignTick()).isEqualTo(6L);

        double dwellX = afterCompletion.getX();
        double dwellY = afterCompletion.getY();

        simulationApplicationService.step(city.getId(), 5);

        Human afterDwell = humanRepository.findById(human.getId()).orElseThrow();
        Optional<HumanGoal> activeDuringDwell = humanGoalApplicationService.findActiveGoal(human.getId());
        assertThat(afterDwell.getX()).isEqualTo(dwellX);
        assertThat(afterDwell.getY()).isEqualTo(dwellY);
        assertThat(activeDuringDwell).isEmpty();

        simulationApplicationService.step(city.getId(), 1);

        Human afterReassignment = humanRepository.findById(human.getId()).orElseThrow();
        HumanGoal reassigned = humanGoalApplicationService.findActiveGoal(human.getId()).orElseThrow();
        List<Event> assignedEvents = eventRepository.findByCityIdAndEventTypeOrderByTickAscSequenceInTickAscIdAsc(
                city.getId(),
                EventType.GOAL_ASSIGNED
        );
        assertThat(reassigned.getSource()).isEqualTo(HumanGoalSource.AUTONOMOUS);
        assertThat(reassigned.getGoalType()).isEqualTo(HumanGoalType.MOVE_TO_PLACE);
        assertThat(reassigned.getTargetPlaceId()).isNotEqualTo("forest");
        assertThat(afterReassignment.getX()).isNotEqualTo(dwellX);
        assertThat(afterReassignment.getY()).isNotEqualTo(dwellY);
        assertThat(afterReassignment.getNextGoalAssignTick()).isNull();
        assertThat(assignedEvents).anyMatch(
                event -> HumanGoalSource.AUTONOMOUS.name().equals(event.getPayload().get("source"))
        );
    }

    @Test
    void humansWithoutGoalMetadataReceiveDeterministicDefaultGoalOnStep() {
        City city = createCity("Goals-Legacy");
        Human human = createHuman(city, "Milo", 0.5, 0.5);
        human.setNextGoalAssignTick(null);
        humanRepository.save(human);
        simulationApplicationService.createRun(city.getId(), 20260321L);

        simulationApplicationService.step(city.getId(), 1);

        HumanGoal active = humanGoalApplicationService.findActiveGoal(human.getId()).orElseThrow();
        Human updated = humanRepository.findById(human.getId()).orElseThrow();
        assertThat(active.getSource()).isEqualTo(HumanGoalSource.AUTONOMOUS);
        assertThat(active.getGoalType()).isEqualTo(HumanGoalType.MOVE_TO_PLACE);
        assertThat(updated.getX()).isNotEqualTo(0.5);
        assertThat(updated.getY()).isNotEqualTo(0.5);
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
