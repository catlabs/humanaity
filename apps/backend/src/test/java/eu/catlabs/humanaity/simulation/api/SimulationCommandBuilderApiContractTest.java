package eu.catlabs.humanaity.simulation.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import eu.catlabs.humanaity.auth.infrastructure.security.JwtService;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.SimulationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:simulation-command-builder-api;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
@AutoConfigureMockMvc
class SimulationCommandBuilderApiContractTest {

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
    private SimulationRunRepository simulationRunRepository;
    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        simulationRunRepository.deleteAll();
        humanRepository.deleteAll();
        cityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void commandBuilderRejectsUnauthenticatedCaller() throws Exception {
        User owner = persistUser("command-builder-owner-unauth@example.com");
        City city = persistCity("Builder City", owner);

        mockMvc.perform(get("/api/simulations/{cityId}/command-builder", city.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void commandBuilderRejectsNonOwner() throws Exception {
        User owner = persistUser("command-builder-owner@example.com");
        User other = persistUser("command-builder-other@example.com");
        City city = persistCity("Builder City", owner);

        mockMvc.perform(get("/api/simulations/{cityId}/command-builder", city.getId())
                        .header("Authorization", bearerFor(other)))
                .andExpect(status().isForbidden());
    }

    @Test
    void commandBuilderReturnsActionMetadataAndTargets() throws Exception {
        User owner = persistUser("command-builder-owner-data@example.com");
        City city = persistCity("Builder City", owner);
        Human ada = persistHuman(city, "Ada", 0.2, 0.3);
        Human ben = persistHuman(city, "Ben", 0.8, 0.7);

        MvcResult result = mockMvc.perform(get("/api/simulations/{cityId}/command-builder", city.getId())
                        .header("Authorization", bearerFor(owner)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.path("actorOptions").isArray()).isTrue();
        assertThat(payload.path("actorOptions").toString()).contains(String.valueOf(ada.getId()), String.valueOf(ben.getId()));
        assertThat(payload.path("actions").isArray()).isTrue();
        assertThat(payload.path("actions").toString()).contains("WORLD_STATUS", "RECENT_EVENTS", "INVENTIONS", "RELATIONSHIPS");
        assertThat(payload.path("actions").toString()).contains("FOCUS_HUMAN", "MOVE_HUMAN_TO_PLACE", "MEET_HUMAN");

        JsonNode meetAction = null;
        for (JsonNode action : payload.path("actions")) {
            if ("MEET_HUMAN".equals(action.path("actionKey").asText())) {
                meetAction = action;
                break;
            }
        }
        assertThat(meetAction).isNotNull();
        assertThat(meetAction.path("executionKind").asText()).isEqualTo("COMMAND");
        assertThat(meetAction.path("actorKind").asText()).isEqualTo("HUMAN");
        assertThat(meetAction.path("targetKind").asText()).isEqualTo("HUMAN");
        assertThat(meetAction.path("requiresDifferentTarget").asBoolean()).isTrue();
        assertThat(meetAction.path("targetOptions").toString()).contains(String.valueOf(ada.getId()), String.valueOf(ben.getId()));
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtService.generateAccessToken(user.getEmail());
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("hash");
        return userRepository.save(user);
    }

    private City persistCity(String name, User owner) {
        City city = new City();
        city.setName(name);
        city.setOwner(owner);
        return cityRepository.save(city);
    }

    private Human persistHuman(City city, String name, double x, double y) {
        Human human = new Human();
        human.setCity(city);
        human.setName(name);
        human.setBusy(false);
        human.setX(x);
        human.setY(y);
        return humanRepository.save(human);
    }
}
