package eu.catlabs.humanaity.simulation.application;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.simulation.domain.HumanActionType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HumanActionCatalogServiceTest {

    private final HumanActionCatalogService service = new HumanActionCatalogService();

    @Test
    void applicationsMapToDeterministicActionCatalog() {
        EnumSet<HumanActionType> catalog = service.actionsForApplications(Set.of(
                "APP_TELL_STORIES",
                "APP_CREATE_ART"
        ));

        assertThat(catalog).contains(
                HumanActionType.TELL_STORIES,
                HumanActionType.CREATE_ART
        );
    }

    @Test
    void sameSeedAndStateSelectSameAction() {
        Human actor = human(1L, "Ada", 0.34, 0.72);
        Human nearby = human(2L, "Ben", 0.35, 0.73);
        List<Human> humans = List.of(actor, nearby);

        EnumSet<HumanActionType> unlockedActions = EnumSet.of(
                HumanActionType.COOK_FOOD,
                HumanActionType.TELL_STORIES
        );
        Optional<HumanActionCatalogService.SelectedHumanAction> left = service.selectAction(
                1234L,
                10L,
                actor,
                humans,
                SimulationPlaceRegistry.byId("forest"),
                unlockedActions,
                false
        );
        Optional<HumanActionCatalogService.SelectedHumanAction> right = service.selectAction(
                1234L,
                10L,
                actor,
                humans,
                SimulationPlaceRegistry.byId("forest"),
                unlockedActions,
                false
        );

        assertThat(left).isPresent();
        assertThat(right).isPresent();
        assertThat(right.get().actionType()).isEqualTo(left.get().actionType());
    }

    @Test
    void activeGoalSuppressesKnowledgeActionSelection() {
        Human actor = human(1L, "Ada", 0.34, 0.72);
        Optional<HumanActionCatalogService.SelectedHumanAction> action = service.selectAction(
                1234L,
                10L,
                actor,
                List.of(actor),
                SimulationPlaceRegistry.byId("forest"),
                EnumSet.of(HumanActionType.CREATE_ART),
                true
        );

        assertThat(action).isEmpty();
    }

    private Human human(Long id, String name, double x, double y) {
        Human human = new Human();
        human.setId(id);
        human.setName(name);
        human.setX(x);
        human.setY(y);
        City city = new City();
        city.setId(1L);
        human.setCity(city);
        return human;
    }
}
