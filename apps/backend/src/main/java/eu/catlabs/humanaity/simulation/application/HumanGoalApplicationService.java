package eu.catlabs.humanaity.simulation.application;

import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.simulation.domain.HumanGoal;
import eu.catlabs.humanaity.simulation.domain.HumanGoalSource;
import eu.catlabs.humanaity.simulation.domain.HumanGoalStatus;
import eu.catlabs.humanaity.simulation.domain.HumanGoalType;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.HumanGoalRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class HumanGoalApplicationService {

    private final HumanGoalRepository humanGoalRepository;
    private final HumanRepository humanRepository;

    public HumanGoalApplicationService(
            HumanGoalRepository humanGoalRepository,
            HumanRepository humanRepository
    ) {
        this.humanGoalRepository = humanGoalRepository;
        this.humanRepository = humanRepository;
    }

    @Transactional
    public HumanGoal assignGoal(
            Long cityId,
            Long humanId,
            HumanGoalType goalType,
            HumanGoalSource source,
            Long assignedTick,
            GoalTarget target
    ) {
        Human human = humanRepository.findById(humanId)
                .filter(candidate -> candidate.getCity() != null && candidate.getCity().getId().equals(cityId))
                .orElseThrow(() -> new EntityNotFoundException("Human not found in city"));

        Optional<HumanGoal> existingActive = humanGoalRepository
                .findFirstByHumanIdAndStatusOrderByAssignedTickDescIdDesc(humanId, HumanGoalStatus.ACTIVE);
        if (existingActive.isPresent()) {
            HumanGoal goal = existingActive.get();
            goal.setStatus(HumanGoalStatus.CANCELLED);
            goal.setCompletedTick(assignedTick);
            humanGoalRepository.save(goal);
        }

        HumanGoal newGoal = new HumanGoal();
        newGoal.setHuman(human);
        newGoal.setGoalType(goalType);
        newGoal.setStatus(HumanGoalStatus.ACTIVE);
        newGoal.setSource(source);
        newGoal.setAssignedTick(assignedTick);
        newGoal.setCompletedTick(null);
        newGoal.setTargetPlaceId(target.targetPlaceId());
        newGoal.setTargetHumanId(target.targetHumanId());
        newGoal.setTargetX(target.targetX());
        newGoal.setTargetY(target.targetY());
        newGoal.setMetadataKey(target.metadataKey());
        return humanGoalRepository.save(newGoal);
    }

    @Transactional
    public Optional<HumanGoal> findActiveGoal(Long humanId) {
        return humanGoalRepository.findFirstByHumanIdAndStatusOrderByAssignedTickDescIdDesc(humanId, HumanGoalStatus.ACTIVE);
    }

    @Transactional
    public List<HumanGoal> listActiveGoalsByCity(Long cityId) {
        return humanGoalRepository.findByHumanCityIdAndStatusOrderByAssignedTickAscIdAsc(cityId, HumanGoalStatus.ACTIVE);
    }

    @Transactional
    public HumanGoal completeGoal(HumanGoal goal, Long completedTick) {
        goal.setStatus(HumanGoalStatus.COMPLETED);
        goal.setCompletedTick(completedTick);
        return humanGoalRepository.save(goal);
    }

    @Transactional
    public HumanGoal cancelGoal(HumanGoal goal, Long completedTick) {
        goal.setStatus(HumanGoalStatus.CANCELLED);
        goal.setCompletedTick(completedTick);
        return humanGoalRepository.save(goal);
    }

    public record GoalTarget(
            String targetPlaceId,
            Long targetHumanId,
            Double targetX,
            Double targetY,
            String metadataKey
    ) {
    }
}
