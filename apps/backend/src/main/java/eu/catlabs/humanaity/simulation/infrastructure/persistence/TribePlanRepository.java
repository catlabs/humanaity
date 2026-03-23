package eu.catlabs.humanaity.simulation.infrastructure.persistence;

import eu.catlabs.humanaity.simulation.domain.TribePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TribePlanRepository extends JpaRepository<TribePlan, Long> {
    List<TribePlan> findByCityIdOrderByTribeIdAsc(Long cityId);
    Optional<TribePlan> findFirstByCityIdAndTribeId(Long cityId, String tribeId);

    long deleteByCityId(Long cityId);
}
