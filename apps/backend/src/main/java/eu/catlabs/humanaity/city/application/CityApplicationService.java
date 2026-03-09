package eu.catlabs.humanaity.city.application;

import eu.catlabs.humanaity.city.api.dto.CityInput;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.human.application.HumanGenerationApplicationService;
import jakarta.persistence.EntityManager;
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

    public City updateCity(String id, CityInput input) {
        Optional<City> existingCity = cityRepository.findById(Long.parseLong(id));

        if (existingCity.isPresent()) {
            City city = existingCity.get();
            city.setName(input.getName());
            return cityRepository.save(city);
        }

        throw new RuntimeException("City not found with id: " + id);
    }

    public void deleteCity(Long id) {
        cityRepository.deleteById(id);
    }

    public List<City> getCitiesForUser(User user) {
        return cityRepository.findByOwner(user);
    }
}
