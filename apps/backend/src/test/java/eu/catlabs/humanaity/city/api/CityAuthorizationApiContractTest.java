package eu.catlabs.humanaity.city.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import eu.catlabs.humanaity.auth.infrastructure.security.JwtService;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.domain.EventCategory;
import eu.catlabs.humanaity.event.domain.EventType;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.history.domain.HistoryEra;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.invention.domain.Invention;
import eu.catlabs.humanaity.invention.domain.InventionCategory;
import eu.catlabs.humanaity.invention.infrastructure.persistence.InventionRepository;
import eu.catlabs.humanaity.simulation.domain.HumanGoal;
import eu.catlabs.humanaity.simulation.domain.HumanGoalSource;
import eu.catlabs.humanaity.simulation.domain.HumanGoalStatus;
import eu.catlabs.humanaity.simulation.domain.HumanGoalType;
import eu.catlabs.humanaity.simulation.domain.KnowledgeUnlock;
import eu.catlabs.humanaity.simulation.domain.SimulationRun;
import eu.catlabs.humanaity.simulation.domain.SimulationRunStatus;
import eu.catlabs.humanaity.simulation.domain.TechTreeNodeType;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.HumanGoalRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.KnowledgeUnlockRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.SimulationRunRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeHouseRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeKnownPlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:city-authz-api;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
@AutoConfigureMockMvc
class CityAuthorizationApiContractTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private HumanRepository humanRepository;
    @Autowired
    private HumanGoalRepository humanGoalRepository;
    @Autowired
    private SimulationRunRepository simulationRunRepository;
    @Autowired
    private KnowledgeUnlockRepository knowledgeUnlockRepository;
    @Autowired
    private TribeHouseRepository tribeHouseRepository;
    @Autowired
    private TribeKnownPlaceRepository tribeKnownPlaceRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private InventionRepository inventionRepository;
    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        tribeKnownPlaceRepository.deleteAll();
        tribeHouseRepository.deleteAll();
        humanGoalRepository.deleteAll();
        eventRepository.deleteAll();
        inventionRepository.deleteAll();
        knowledgeUnlockRepository.deleteAll();
        simulationRunRepository.deleteAll();
        humanRepository.deleteAll();
        cityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void updateCityRejectsUnauthenticatedCaller() throws Exception {
        User owner = persistUser("owner-update-unauth@example.com");
        City city = persistCity("OwnerTown", owner);

        mockMvc.perform(put("/api/cities/{id}", city.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cityInput("Unauthorized Update")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listAllCitiesReturnsSharedCitiesForAuthenticatedUser() throws Exception {
        User owner = persistUser("owner-list@example.com");
        User otherUser = persistUser("other-list@example.com");
        City city = persistCity("SharedTown", owner);

        String payload = mockMvc.perform(get("/api/cities")
                        .header("Authorization", bearerFor(otherUser)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(payload).contains(city.getId().toString(), "SharedTown");
    }

    @Test
    void legacyMineAliasReturnsSharedCitiesForAuthenticatedUser() throws Exception {
        User owner = persistUser("owner-mine@example.com");
        User otherUser = persistUser("other-mine@example.com");
        City city = persistCity("SharedMineTown", owner);

        String payload = mockMvc.perform(get("/api/cities/mine")
                        .header("Authorization", bearerFor(otherUser)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(payload).contains(city.getId().toString(), "SharedMineTown");
    }

    @Test
    void updateCityAllowsAnyAuthenticatedUser() throws Exception {
        User owner = persistUser("owner-update-forbidden@example.com");
        User otherUser = persistUser("other-update-forbidden@example.com");
        City city = persistCity("OwnerTown", owner);

        mockMvc.perform(put("/api/cities/{id}", city.getId())
                        .header("Authorization", bearerFor(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cityInput("Hijacked City")))
                .andExpect(status().isOk());

        City updated = cityRepository.findById(city.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Hijacked City");
    }

    @Test
    void updateCityAllowsOwner() throws Exception {
        User owner = persistUser("owner-update-ok@example.com");
        City city = persistCity("InitialName", owner);

        mockMvc.perform(put("/api/cities/{id}", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cityInput("RenamedCity")))
                .andExpect(status().isOk());

        City updated = cityRepository.findById(city.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("RenamedCity");
    }

    @Test
    void updateCityReturnsNotFoundWhenCityDoesNotExist() throws Exception {
        User owner = persistUser("owner-update-missing@example.com");

        mockMvc.perform(put("/api/cities/{id}", 999_999L)
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cityInput("MissingCity")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCityRejectsUnauthenticatedCaller() throws Exception {
        User owner = persistUser("owner-delete-unauth@example.com");
        City city = persistCity("DeleteTown", owner);

        mockMvc.perform(delete("/api/cities/{id}", city.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteCityAllowsAnyAuthenticatedUser() throws Exception {
        User owner = persistUser("owner-delete-forbidden@example.com");
        User otherUser = persistUser("other-delete-forbidden@example.com");
        City city = persistCity("DeleteTown", owner);
        seedCityDependencies(city);

        mockMvc.perform(delete("/api/cities/{id}", city.getId())
                        .header("Authorization", bearerFor(otherUser)))
                .andExpect(status().isNoContent());

        assertThat(cityRepository.findById(city.getId())).isEmpty();
    }

    @Test
    void deleteCityAllowsOwner() throws Exception {
        User owner = persistUser("owner-delete-ok@example.com");
        City city = persistCity("DeleteTown", owner);
        seedCityDependencies(city);

        mockMvc.perform(delete("/api/cities/{id}", city.getId())
                        .header("Authorization", bearerFor(owner)))
                .andExpect(status().isNoContent());

        assertThat(cityRepository.findById(city.getId())).isEmpty();
        assertThat(humanRepository.findByCityId(city.getId())).isEmpty();
        assertThat(simulationRunRepository.findByCityId(city.getId())).isEmpty();
        assertThat(knowledgeUnlockRepository.findByCityIdOrderByUnlockedTickAscNodeIdAsc(city.getId())).isEmpty();
        assertThat(eventRepository.findByCityIdOrderByTickAscSequenceInTickAscIdAsc(city.getId())).isEmpty();
        assertThat(inventionRepository.findByCityIdOrderByTickCreatedAscInventionKeyAscIdAsc(city.getId())).isEmpty();
    }

    @Test
    void deleteCityReturnsNotFoundWhenCityDoesNotExist() throws Exception {
        User owner = persistUser("owner-delete-missing@example.com");

        mockMvc.perform(delete("/api/cities/{id}", 999_999L)
                        .header("Authorization", bearerFor(owner)))
                .andExpect(status().isNotFound());
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("hashed-password");
        user.setRoles(Set.of("ROLE_USER"));
        return userRepository.save(user);
    }

    private City persistCity(String name, User owner) {
        City city = new City();
        city.setName(name);
        city.setOwner(owner);
        return cityRepository.save(city);
    }

    private void seedCityDependencies(City city) {
        Human human = new Human();
        human.setName("Alice");
        human.setCity(city);
        human.setBusy(false);
        human.setTribeId("tribe-1");
        human.setX(0.0);
        human.setY(0.0);
        human = humanRepository.save(human);

        HumanGoal goal = new HumanGoal();
        goal.setHuman(human);
        goal.setGoalType(HumanGoalType.MOVE_TO_PLACE);
        goal.setStatus(HumanGoalStatus.ACTIVE);
        goal.setSource(HumanGoalSource.AUTONOMOUS);
        goal.setAssignedTick(1L);
        humanGoalRepository.save(goal);

        SimulationRun run = new SimulationRun();
        run.setCity(city);
        run.setSeed(1234L);
        run.setTick(2L);
        run.setStatus(SimulationRunStatus.RUNNING);
        simulationRunRepository.save(run);

        KnowledgeUnlock unlock = new KnowledgeUnlock();
        unlock.setCity(city);
        unlock.setNodeId("discovery.fire");
        unlock.setNodeType(TechTreeNodeType.DISCOVERY);
        unlock.setUnlockedTick(2L);
        knowledgeUnlockRepository.save(unlock);

        Event event = new Event();
        event.setCity(city);
        event.setTick(2L);
        event.setSequenceInTick(1);
        event.setEventCategory(EventCategory.DISCOVERY);
        event.setEventType(EventType.DISCOVERY_UNLOCKED);
        event.setImportance(5);
        event.setYear(1);
        event.setEra(HistoryEra.FOUNDING);
        event.setEventKey("evt-discovery-fire");
        eventRepository.save(event);

        Invention invention = new Invention();
        invention.setCity(city);
        invention.setTickCreated(3L);
        invention.setCategory(InventionCategory.KNOWLEDGE);
        invention.setInventionKey("inv-fire-control");
        invention.setTitle("Fire Control");
        invention.setSummary("Basic control over fire.");
        invention.setImpactScore(8);
        invention.setYearCreated(1);
        invention.setEraCreated(HistoryEra.FOUNDING);
        inventionRepository.save(invention);
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtService.generateAccessToken(user.getEmail());
    }

    private String cityInput(String name) throws Exception {
        return objectMapper.writeValueAsString(new CityInputPayload(name));
    }

    private record CityInputPayload(String name) {
    }
}
