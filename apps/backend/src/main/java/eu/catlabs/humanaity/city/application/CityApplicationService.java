package eu.catlabs.humanaity.city.application;

import eu.catlabs.humanaity.city.api.dto.CityInput;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.human.application.HumanGenerationApplicationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CityApplicationService {
    private final CityRepository cityRepository;
    private final HumanGenerationApplicationService humanGenerationService;
    private final EntityManager entityManager;

    public CityApplicationService(CityRepository cityRepository,
                                  HumanGenerationApplicationService humanGenerationService,
                                  EntityManager entityManager) {
        this.cityRepository = cityRepository;
        this.humanGenerationService = humanGenerationService;
        this.entityManager = entityManager;
    }

    public List<City> getAllCities() {
        return cityRepository.findAll();
    }

    public Optional<City> getCityById(String id) {
        return cityRepository.findById(Long.parseLong(id));
    }

    public List<City> getCitiesByName(String name) {
        return cityRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional
    public City createCityForUser(CityInput input, User owner) {
        if (owner == null) {
            throw new IllegalArgumentException("Owner cannot be null");
        }
        
        City city = new City();
        city.setName(input.getName());
        city.setOwner(owner);
        
        City savedCity = cityRepository.save(city);
        
        // Flush to ensure the city is persisted before generating humans
        entityManager.flush();
        
        // Generate humans for the city
        humanGenerationService.generateHumansForCity(savedCity);
        
        return savedCity;
    }

    public City updateCity(Long id, CityInput input, User currentUser) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + id));
        enforceOwnership(city, currentUser);
        city.setName(input.getName());
        return cityRepository.save(city);
    }

    public void deleteCity(Long id, User currentUser) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + id));
        enforceOwnership(city, currentUser);
        cityRepository.delete(city);
    }

    public List<City> getCitiesForUser(User user) {
        return cityRepository.findByOwner(user);
    }

    private void enforceOwnership(City city, User currentUser) {
        if (city.getOwner() == null || city.getOwner().getId() == null || currentUser == null || currentUser.getId() == null) {
            throw new AccessDeniedException("City ownership cannot be verified");
        }
        if (!city.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not own this city");
        }
    }
}
