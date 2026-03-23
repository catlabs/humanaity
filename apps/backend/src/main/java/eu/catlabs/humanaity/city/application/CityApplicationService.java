package eu.catlabs.humanaity.city.application;

import eu.catlabs.humanaity.city.api.dto.CityInput;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.human.application.HumanGenerationApplicationService;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.invention.infrastructure.persistence.InventionRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.DirectorInterventionRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.HumanGoalRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.KnowledgeUnlockRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.SimulationRunRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeHouseRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeKnownPlaceRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CityApplicationService {
    private final CityRepository cityRepository;
    private final HumanGenerationApplicationService humanGenerationService;
    private final CityTribeBootstrapService cityTribeBootstrapService;
    private final EntityManager entityManager;
    private final SimulationRunRepository simulationRunRepository;
    private final KnowledgeUnlockRepository knowledgeUnlockRepository;
    private final TribeHouseRepository tribeHouseRepository;
    private final TribeKnownPlaceRepository tribeKnownPlaceRepository;
    private final EventRepository eventRepository;
    private final InventionRepository inventionRepository;
    private final HumanGoalRepository humanGoalRepository;
    private final HumanRepository humanRepository;
    private final DirectorInterventionRepository directorInterventionRepository;

    public CityApplicationService(CityRepository cityRepository,
                                  HumanGenerationApplicationService humanGenerationService,
                                  CityTribeBootstrapService cityTribeBootstrapService,
                                  EntityManager entityManager,
                                  SimulationRunRepository simulationRunRepository,
                                  KnowledgeUnlockRepository knowledgeUnlockRepository,
                                  TribeHouseRepository tribeHouseRepository,
                                  TribeKnownPlaceRepository tribeKnownPlaceRepository,
                                  EventRepository eventRepository,
                                  InventionRepository inventionRepository,
                                  HumanGoalRepository humanGoalRepository,
                                  HumanRepository humanRepository,
                                  DirectorInterventionRepository directorInterventionRepository) {
        this.cityRepository = cityRepository;
        this.humanGenerationService = humanGenerationService;
        this.cityTribeBootstrapService = cityTribeBootstrapService;
        this.entityManager = entityManager;
        this.simulationRunRepository = simulationRunRepository;
        this.knowledgeUnlockRepository = knowledgeUnlockRepository;
        this.tribeHouseRepository = tribeHouseRepository;
        this.tribeKnownPlaceRepository = tribeKnownPlaceRepository;
        this.eventRepository = eventRepository;
        this.inventionRepository = inventionRepository;
        this.humanGoalRepository = humanGoalRepository;
        this.humanRepository = humanRepository;
        this.directorInterventionRepository = directorInterventionRepository;
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
        City city = new City();
        city.setName(input.getName());
        city.setOwner(owner);

        City savedCity = cityRepository.save(city);

        // Flush to ensure the city is persisted before generating humans
        entityManager.flush();

        // Create tribe houses before generating humans so the population can spawn around homes.
        cityTribeBootstrapService.ensureTribeHouses(savedCity);

        // Generate humans for the city
        humanGenerationService.generateHumansForCity(savedCity);

        return savedCity;
    }

    public City updateCity(Long id, CityInput input, User currentUser) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + id));
        city.setName(input.getName());
        return cityRepository.save(city);
    }

    @Transactional
    public void deleteCity(Long id, User currentUser) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + id));

        // Remove dependent rows that reference city/human foreign keys before deleting the city.
        humanGoalRepository.deleteByHumanCityId(id);
        eventRepository.deleteByCityId(id);
        inventionRepository.deleteByCityId(id);
        knowledgeUnlockRepository.deleteByCityId(id);
        tribeKnownPlaceRepository.deleteByCityId(id);
        tribeHouseRepository.deleteByCityId(id);
        simulationRunRepository.deleteByCityId(id);
        directorInterventionRepository.deleteByCityId(id);
        humanRepository.deleteByCityId(id);

        cityRepository.delete(city);
    }

    public List<City> getCitiesForUser(User user) {
        return cityRepository.findAll();
    }
}
