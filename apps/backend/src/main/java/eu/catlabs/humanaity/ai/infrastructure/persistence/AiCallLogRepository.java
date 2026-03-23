package eu.catlabs.humanaity.ai.infrastructure.persistence;

import eu.catlabs.humanaity.ai.domain.AiCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiCallLogRepository extends JpaRepository<AiCallLog, Long> {
}
