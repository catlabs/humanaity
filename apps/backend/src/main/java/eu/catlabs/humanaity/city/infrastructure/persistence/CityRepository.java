package eu.catlabs.humanaity.city.infrastructure.persistence;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findByNameContainingIgnoreCase(String name);
    List<City> findByOwner(User owner);
}
