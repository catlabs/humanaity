package eu.catlabs.humanaity.simulation.infrastructure.persistence;

import eu.catlabs.humanaity.simulation.domain.HumanGoal;
import eu.catlabs.humanaity.simulation.domain.HumanGoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HumanGoalRepository extends JpaRepository<HumanGoal, Long> {
    Optional<HumanGoal> findFirstByHumanIdAndStatusOrderByAssignedTickDescIdDesc(Long humanId, HumanGoalStatus status);
    List<HumanGoal> findByHumanCityIdAndStatusOrderByAssignedTickAscIdAsc(Long cityId, HumanGoalStatus status);
    long countByHumanCityIdAndStatusAndMetadataKey(Long cityId, HumanGoalStatus status, String metadataKey);

    long deleteByHumanCityId(Long cityId);
}
