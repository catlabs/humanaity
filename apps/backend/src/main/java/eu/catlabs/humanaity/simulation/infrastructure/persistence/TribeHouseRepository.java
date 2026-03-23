package eu.catlabs.humanaity.simulation.infrastructure.persistence;

import eu.catlabs.humanaity.simulation.domain.TribeHouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TribeHouseRepository extends JpaRepository<TribeHouse, Long> {
    List<TribeHouse> findByCityIdOrderByTribeIdAsc(Long cityId);

    Optional<TribeHouse> findFirstByCityIdAndTribeId(Long cityId, String tribeId);

    long deleteByCityId(Long cityId);
}
