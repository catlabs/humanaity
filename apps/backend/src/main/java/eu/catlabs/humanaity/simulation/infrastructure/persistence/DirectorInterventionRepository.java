package eu.catlabs.humanaity.simulation.infrastructure.persistence;

import eu.catlabs.humanaity.simulation.domain.DirectorIntervention;
import eu.catlabs.humanaity.simulation.domain.DirectorInterventionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DirectorInterventionRepository extends JpaRepository<DirectorIntervention, Long> {
    Optional<DirectorIntervention> findByConfirmationTokenAndStatus(
            String confirmationToken,
            DirectorInterventionStatus status
    );

    long deleteByCityId(Long cityId);
}
