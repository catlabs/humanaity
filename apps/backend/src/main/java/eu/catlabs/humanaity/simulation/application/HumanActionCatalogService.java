package eu.catlabs.humanaity.simulation.application;

import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.simulation.domain.HumanActionType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Service
public class HumanActionCatalogService {

    public EnumSet<HumanActionType> actionsForApplications(Set<String> unlockedApplicationIds) {
        EnumSet<HumanActionType> actions = EnumSet.noneOf(HumanActionType.class);
        if (unlockedApplicationIds.contains("APP_TELL_STORIES")) {
            actions.add(HumanActionType.TELL_STORIES);
        }
        if (unlockedApplicationIds.contains("APP_CREATE_ART")) {
            actions.add(HumanActionType.CREATE_ART);
        }
        if (unlockedApplicationIds.contains("APP_TRADE_GOODS")) {
            actions.add(HumanActionType.TRADE_GOODS);
        }
        return actions;
    }

    public Optional<SelectedHumanAction> selectAction(
            long runSeed,
            long tick,
            Human actor,
            List<Human> allHumans,
            Optional<SimulationPlaceRegistry.SimulationPlace> currentPlace,
            EnumSet<HumanActionType> unlockedActions,
            boolean hasActiveGoal
    ) {
        if (hasActiveGoal || unlockedActions.isEmpty()) {
            return Optional.empty();
        }

        List<HumanActionType> eligible = new ArrayList<>();
        for (HumanActionType actionType : unlockedActions) {
            if (isEligible(actionType, actor, allHumans, currentPlace)) {
                eligible.add(actionType);
            }
        }
        if (eligible.isEmpty()) {
            return Optional.empty();
        }

        eligible.sort(Comparator.comparing(Enum::name));
        Random random = new Random(deriveActionSeed(runSeed, tick, actor.getId()));
        HumanActionType selected = eligible.get(random.nextInt(eligible.size()));
        return Optional.of(new SelectedHumanAction(actor.getId(), selected));
    }

    private boolean isEligible(
            HumanActionType actionType,
            Human actor,
            List<Human> allHumans,
            Optional<SimulationPlaceRegistry.SimulationPlace> currentPlace
    ) {
        return switch (actionType) {
            case TELL_STORIES -> hasNearbyHuman(actor, allHumans, 0.18);
            case CREATE_ART -> true;
            case TRADE_GOODS -> hasNearbyHuman(actor, allHumans, 0.15);
            case COOK_FOOD, STORE_FOOD -> false;
        };
    }

    private boolean hasNearbyHuman(Human actor, List<Human> allHumans, double threshold) {
        for (Human other : allHumans) {
            if (other.getId().equals(actor.getId())) {
                continue;
            }
            double deltaX = actor.getX() - other.getX();
            double deltaY = actor.getY() - other.getY();
            double distance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
            if (distance <= threshold) {
                return true;
            }
        }
        return false;
    }

    private long deriveActionSeed(long runSeed, long tick, Long humanId) {
        long mixed = runSeed;
        mixed = (mixed * 31L) + tick;
        mixed = (mixed * 31L) + humanId;
        mixed ^= 0x3C79AC492BA7B653L;
        return mixed;
    }

    public record SelectedHumanAction(Long humanId, HumanActionType actionType) {
    }
}
