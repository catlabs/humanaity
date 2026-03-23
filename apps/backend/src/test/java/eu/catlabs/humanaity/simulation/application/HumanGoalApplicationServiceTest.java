package eu.catlabs.humanaity.simulation.application;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.simulation.domain.HumanGoal;
import eu.catlabs.humanaity.simulation.domain.HumanGoalSource;
import eu.catlabs.humanaity.simulation.domain.HumanGoalStatus;
import eu.catlabs.humanaity.simulation.domain.HumanGoalType;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.HumanGoalRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeHouseRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeKnownPlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:human-goals;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
class HumanGoalApplicationServiceTest {

    @Autowired
    private HumanGoalApplicationService humanGoalApplicationService;
    @Autowired
    private HumanGoalRepository humanGoalRepository;
    @Autowired
    private HumanRepository humanRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private TribeHouseRepository tribeHouseRepository;
    @Autowired
    private TribeKnownPlaceRepository tribeKnownPlaceRepository;

    @BeforeEach
    void cleanDatabase() {
        tribeKnownPlaceRepository.deleteAll();
        tribeHouseRepository.deleteAll();
        humanGoalRepository.deleteAll();
        humanRepository.deleteAll();
        cityRepository.deleteAll();
    }

    @Test
    void assignGoalStoresCanonicalGoalPayloadAndProvenance() {
        City city = createCity("Goals-A");
        Human human = createHuman(city, "Elsa", 0.2, 0.3);
        human.setNextGoalAssignTick(42L);
        human = humanRepository.save(human);

        HumanGoal goal = humanGoalApplicationService.assignGoal(
                city.getId(),
                human.getId(),
                HumanGoalType.MOVE_TO_PLACE,
                HumanGoalSource.CHAT_COMMAND,
                12L,
                new HumanGoalApplicationService.GoalTarget("forest", null, 0.14, 0.18, "chat:msg-1")
        );

        assertThat(goal.getId()).isNotNull();
        assertThat(goal.getHuman().getId()).isEqualTo(human.getId());
        assertThat(goal.getGoalType()).isEqualTo(HumanGoalType.MOVE_TO_PLACE);
        assertThat(goal.getStatus()).isEqualTo(HumanGoalStatus.ACTIVE);
        assertThat(goal.getSource()).isEqualTo(HumanGoalSource.CHAT_COMMAND);
        assertThat(goal.getAssignedTick()).isEqualTo(12L);
        assertThat(goal.getTargetPlaceId()).isEqualTo("forest");
        assertThat(goal.getTargetX()).isEqualTo(0.14);
        assertThat(goal.getTargetY()).isEqualTo(0.18);
        assertThat(goal.getMetadataKey()).isEqualTo("chat:msg-1");
        assertThat(humanRepository.findById(human.getId()).orElseThrow().getNextGoalAssignTick()).isNull();
    }

    @Test
    void assignGoalCancelsPreviousActiveGoalForSameHuman() {
        City city = createCity("Goals-B");
        Human human = createHuman(city, "Pierre", 0.4, 0.5);

        HumanGoal first = humanGoalApplicationService.assignGoal(
                city.getId(),
                human.getId(),
                HumanGoalType.MOVE_TO_PLACE,
                HumanGoalSource.AUTONOMOUS,
                5L,
                new HumanGoalApplicationService.GoalTarget("river", null, 0.82, 0.22, "auto-1")
        );

        HumanGoal second = humanGoalApplicationService.assignGoal(
                city.getId(),
                human.getId(),
                HumanGoalType.MEET_HUMAN,
                HumanGoalSource.CHAT_COMMAND,
                9L,
                new HumanGoalApplicationService.GoalTarget(null, human.getId(), null, null, "chat-2")
        );

        List<HumanGoal> goals = humanGoalRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(HumanGoal::getAssignedTick))
                .toList();
        assertThat(goals).hasSize(2);
        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(goals.get(0).getStatus()).isEqualTo(HumanGoalStatus.CANCELLED);
        assertThat(goals.get(0).getCompletedTick()).isEqualTo(9L);
        assertThat(goals.get(1).getStatus()).isEqualTo(HumanGoalStatus.ACTIVE);
        assertThat(goals.get(1).getGoalType()).isEqualTo(HumanGoalType.MEET_HUMAN);
        assertThat(goals.get(1).getTargetHumanId()).isEqualTo(human.getId());
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
}
