package eu.catlabs.humanaity.simulation.application.tribe;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.simulation.domain.HumanGoal;
import eu.catlabs.humanaity.simulation.domain.HumanGoalStatus;
import eu.catlabs.humanaity.simulation.domain.TribeDecisionSource;
import eu.catlabs.humanaity.simulation.domain.TribePlan;
import eu.catlabs.humanaity.simulation.domain.TribePlanStatus;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.HumanGoalRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribePlanRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TribePlanApplicationService {

    private final TribePlanRepository tribePlanRepository;
    private final HumanGoalRepository humanGoalRepository;
    private final CityRepository cityRepository;

    public TribePlanApplicationService(
            TribePlanRepository tribePlanRepository,
            HumanGoalRepository humanGoalRepository,
            CityRepository cityRepository
    ) {
        this.tribePlanRepository = tribePlanRepository;
        this.humanGoalRepository = humanGoalRepository;
        this.cityRepository = cityRepository;
    }

    @Transactional
    public TribePlan activatePlan(
            Long cityId,
            String tribeId,
            Long chiefHumanId,
            TribeDecisionType planType,
            String targetPlaceId,
            List<Long> assignedHumanIds,
            TribeDecisionSource decisionSource,
            String reasonSummary,
            String planMetadataKey,
            long tick
    ) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + cityId));

        TribePlan plan = tribePlanRepository.findFirstByCityIdAndTribeId(cityId, tribeId)
                .orElseGet(TribePlan::new);
        plan.setCity(city);
        plan.setTribeId(tribeId);
        plan.setChiefHumanId(chiefHumanId);
        plan.setPlanType(planType);
        plan.setPlanStatus(TribePlanStatus.ACTIVE);
        plan.setTargetPlaceId(targetPlaceId);
        plan.setAssignedHumanIds(assignedHumanIds == null ? new ArrayList<>() : new ArrayList<>(assignedHumanIds));
        plan.setDecisionSource(decisionSource);
        plan.setReasonSummary(trimToLength(reasonSummary, 500));
        plan.setPlanMetadataKey(trimToLength(planMetadataKey, 128));
        plan.setLastAssignedTick(tick);
        plan.setCompletedTick(null);
        return tribePlanRepository.save(plan);
    }

    @Transactional
    public void updatePlanStatusForGoal(HumanGoal goal, HumanGoalStatus goalStatus, long tick) {
        if (goal == null || goal.getHuman() == null || goal.getHuman().getCity() == null) {
            return;
        }
        String tribeId = goal.getHuman().getTribeId();
        String metadataKey = goal.getMetadataKey();
        if (tribeId == null || tribeId.isBlank() || metadataKey == null || metadataKey.isBlank()) {
            return;
        }

        TribePlan plan = tribePlanRepository.findFirstByCityIdAndTribeId(goal.getHuman().getCity().getId(), tribeId)
                .orElse(null);
        if (plan == null || plan.getPlanStatus() != TribePlanStatus.ACTIVE) {
            return;
        }
        if (!metadataKey.equals(plan.getPlanMetadataKey())) {
            return;
        }

        long remainingActive = humanGoalRepository.countByHumanCityIdAndStatusAndMetadataKey(
                goal.getHuman().getCity().getId(),
                HumanGoalStatus.ACTIVE,
                metadataKey
        );
        if (remainingActive > 0) {
            return;
        }

        plan.setPlanStatus(goalStatus == HumanGoalStatus.CANCELLED ? TribePlanStatus.CANCELLED : TribePlanStatus.COMPLETED);
        plan.setCompletedTick(tick);
        tribePlanRepository.save(plan);
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
