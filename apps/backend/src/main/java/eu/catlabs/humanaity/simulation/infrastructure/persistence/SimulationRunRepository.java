package eu.catlabs.humanaity.simulation.infrastructure.persistence;

import eu.catlabs.humanaity.simulation.domain.SimulationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SimulationRunRepository extends JpaRepository<SimulationRun, Long> {
    Optional<SimulationRun> findByCityId(Long cityId);
}
