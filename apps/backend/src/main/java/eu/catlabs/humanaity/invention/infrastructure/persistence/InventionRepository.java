package eu.catlabs.humanaity.invention.infrastructure.persistence;

import eu.catlabs.humanaity.invention.domain.Invention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventionRepository extends JpaRepository<Invention, Long> {
    boolean existsByCityIdAndInventionKey(Long cityId, String inventionKey);

    List<Invention> findByCityIdOrderByTickCreatedAscInventionKeyAscIdAsc(Long cityId);

    List<Invention> findByCityIdAndTickCreatedBetweenOrderByTickCreatedAscInventionKeyAscIdAsc(
            Long cityId,
            Long fromTick,
            Long toTick
    );
}
