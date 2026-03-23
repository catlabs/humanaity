package eu.catlabs.humanaity.simulation.application;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.domain.EventType;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.invention.infrastructure.persistence.InventionRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.HumanGoalRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.KnowledgeUnlockRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.SimulationRunRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeHouseRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeKnownPlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:turn-pacing;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
class SimulationTurnPacingTest {

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
    private TribeHouseRepository tribeHouseRepository;
    @Autowired
    private TribeKnownPlaceRepository tribeKnownPlaceRepository;
    @Autowired
    private HumanRepository humanRepository;
    @Autowired
    private CityRepository cityRepository;

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
    void eachTickEmitsOnlyBoundedOutcomeCount() {
        City city = createCityWithClusteredHumans();
        simulationApplicationService.createRun(city.getId(), 20262424L);
        simulationApplicationService.step(city.getId(), 40);

        List<Event> events = eventRepository.findByCityIdOrderByTickAscSequenceInTickAscIdAsc(city.getId());
        Map<Long, Long> actionEventsPerTick = events.stream()
                .filter(event -> event.getEventType() == EventType.HUMAN_ACTION_PERFORMED)
                .collect(Collectors.groupingBy(Event::getTick, Collectors.counting()));
        Set<EventType> boundedEventTypes = Set.of(
                EventType.DIALOGUE_EXCHANGED,
                EventType.DISCOVERY_UNLOCKED
        );
        Map<Long, Long> boundedOutcomesPerTick = events.stream()
                .filter(event -> boundedEventTypes.contains(event.getEventType()))
                .collect(Collectors.groupingBy(Event::getTick, Collectors.counting()));
        Map<Long, Long> goalEventsPerTick = events.stream()
                .filter(event -> event.getEventType() == EventType.GOAL_ASSIGNED
                        || event.getEventType() == EventType.GOAL_COMPLETED)
                .collect(Collectors.groupingBy(Event::getTick, Collectors.counting()));

        assertThat(events).isNotEmpty();
        assertThat(actionEventsPerTick.values()).allMatch(count -> count <= 1L);
        assertThat(boundedOutcomesPerTick.values()).allMatch(count -> count <= 3L);
        assertThat(goalEventsPerTick.values()).allMatch(count -> count <= 2L);
    }

    private City createCityWithClusteredHumans() {
        City city = new City();
        city.setName("Pacing City");
        City saved = cityRepository.save(city);
        humanRepository.save(createHuman(saved, "A", 0.40, 0.40));
        humanRepository.save(createHuman(saved, "B", 0.42, 0.41));
        humanRepository.save(createHuman(saved, "C", 0.44, 0.42));
        humanRepository.save(createHuman(saved, "D", 0.46, 0.43));
        return saved;
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
}
