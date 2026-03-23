package eu.catlabs.humanaity.simulation.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import eu.catlabs.humanaity.city.api.dto.CityInput;
import eu.catlabs.humanaity.city.application.CityApplicationService;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.invention.infrastructure.persistence.InventionRepository;
import eu.catlabs.humanaity.simulation.application.SimulationApplicationService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tribe-exploration-api;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
@AutoConfigureMockMvc(addFilters = false)
class SimulationTribeExplorationApiContractTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CityApplicationService cityApplicationService;
    @Autowired
    private SimulationApplicationService simulationApplicationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private HumanRepository humanRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private InventionRepository inventionRepository;
    @Autowired
    private SimulationRunRepository simulationRunRepository;
    @Autowired
    private HumanGoalRepository humanGoalRepository;
    @Autowired
    private KnowledgeUnlockRepository knowledgeUnlockRepository;
    @Autowired
    private TribeHouseRepository tribeHouseRepository;
    @Autowired
    private TribeKnownPlaceRepository tribeKnownPlaceRepository;

    @BeforeEach
    void cleanDatabase() {
        tribeKnownPlaceRepository.deleteAll();
        tribeHouseRepository.deleteAll();
        knowledgeUnlockRepository.deleteAll();
        humanGoalRepository.deleteAll();
        inventionRepository.deleteAll();
        eventRepository.deleteAll();
        simulationRunRepository.deleteAll();
        humanRepository.deleteAll();
        cityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void tribeDiscoveryReportAndChiefPlanAppearInSnapshotAndHistory() throws Exception {
        User owner = persistUser("tribe-owner@example.com");
        City city = cityApplicationService.createCityForUser(cityInput("Tribe City"), owner);

        simulationApplicationService.createRun(city.getId(), 424242L);
        simulationApplicationService.step(city.getId(), 120);

        MvcResult snapshotResult = mockMvc.perform(get("/api/simulations/{cityId}/snapshot", city.getId()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode snapshot = objectMapper.readTree(snapshotResult.getResponse().getContentAsString());

        assertThat(snapshot.get("tribes").isArray()).isTrue();
        assertThat(snapshot.get("tribes").size()).isEqualTo(2);
        assertThat(snapshot.get("tribes").toString()).contains("tribe-a", "tribe-b");
        assertThat(snapshot.get("tribes").toString()).contains("knownPlaces");
        assertThat(snapshot.get("humans").toString()).contains("CHIEF");
        assertThat(snapshot.get("humans").toString()).contains("SCOUT");

        JsonNode tribeA = snapshot.get("tribes").get(0);
        assertThat(tribeA.get("house").get("x").asDouble()).isBetween(0.0, 1.0);
        assertThat(tribeA.get("house").get("y").asDouble()).isBetween(0.0, 1.0);
        assertThat(tribeA.get("knownPlaces").isArray()).isTrue();
        assertThat(tribeA.get("scoutHumanId").isNumber()).isTrue();

        MvcResult historyResult = mockMvc.perform(get("/api/simulations/{cityId}/history/events", city.getId())
                        .param("fromTick", "0")
                        .param("limit", "500"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode history = objectMapper.readTree(historyResult.getResponse().getContentAsString());
        assertThat(history.isArray()).isTrue();
        assertThat(history.toString()).contains(
                "TRIBE_PLACE_DISCOVERED",
                "TRIBE_SCOUT_REPORT",
                "TRIBE_PLAN_CHOSEN",
                "TRIBE_DISCOVERY_REPORTED"
        );
    }

    private CityInput cityInput(String name) {
        CityInput input = new CityInput();
        input.setName(name);
        return input;
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("hash");
        return userRepository.save(user);
    }
}
