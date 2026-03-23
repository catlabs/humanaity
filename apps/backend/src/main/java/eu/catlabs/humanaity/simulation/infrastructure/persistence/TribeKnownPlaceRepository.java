package eu.catlabs.humanaity.simulation.infrastructure.persistence;

import eu.catlabs.humanaity.simulation.domain.TribeKnownPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TribeKnownPlaceRepository extends JpaRepository<TribeKnownPlace, Long> {
    List<TribeKnownPlace> findByCityIdAndTribeIdOrderByDiscoveredTickAscIdAsc(Long cityId, String tribeId);

    List<TribeKnownPlace> findByCityIdOrderByTribeIdAscDiscoveredTickAscIdAsc(Long cityId);

    Optional<TribeKnownPlace> findFirstByCityIdAndTribeIdAndPlaceId(Long cityId, String tribeId, String placeId);

    long deleteByCityId(Long cityId);
}
