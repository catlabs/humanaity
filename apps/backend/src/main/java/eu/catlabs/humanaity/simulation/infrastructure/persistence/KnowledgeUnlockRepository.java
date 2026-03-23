package eu.catlabs.humanaity.simulation.infrastructure.persistence;

import eu.catlabs.humanaity.simulation.domain.KnowledgeUnlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeUnlockRepository extends JpaRepository<KnowledgeUnlock, Long> {
    List<KnowledgeUnlock> findByCityIdOrderByUnlockedTickAscNodeIdAsc(Long cityId);
    long deleteByCityId(Long cityId);
}
