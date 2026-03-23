package eu.catlabs.humanaity.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import eu.catlabs.humanaity.auth.infrastructure.security.JwtService;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.invention.infrastructure.persistence.InventionRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:abuse-protection-api;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key",
        "humanaity.abuse-protection.city-create.limit=1",
        "humanaity.abuse-protection.city-create.window-seconds=3600",
        "humanaity.abuse-protection.simulation-mutation.limit=1",
        "humanaity.abuse-protection.simulation-mutation.window-seconds=3600",
        "humanaity.abuse-protection.simulation-assistant.limit=1",
        "humanaity.abuse-protection.simulation-assistant.window-seconds=3600",
        "humanaity.abuse-protection.agent-chat.limit=1",
        "humanaity.abuse-protection.agent-chat.window-seconds=3600"
})
@AutoConfigureMockMvc
class AbuseProtectionApiContractTest {

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
    private HumanGoalRepository humanGoalRepository;
    @Autowired
    private KnowledgeUnlockRepository knowledgeUnlockRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private InventionRepository inventionRepository;
    @Autowired
    private TribeHouseRepository tribeHouseRepository;
    @Autowired
    private TribeKnownPlaceRepository tribeKnownPlaceRepository;
    @Autowired
    private JwtService jwtService;

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
    void cityCreationIsRateLimited() throws Exception {
        User user = persistUser("rate-limit-city@example.com");

        mockMvc.perform(post("/api/cities")
                        .header("Authorization", bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CityPayload("Rate Limited City One"))))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/cities")
                        .header("Authorization", bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CityPayload("Rate Limited City Two"))))
                .andExpect(status().isTooManyRequests())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("Too many city-create requests");
    }

    @Test
    void simulationMutationIsRateLimited() throws Exception {
        User user = persistUser("rate-limit-simulation@example.com");
        City city = persistCity("Rate Simulation", user);

        mockMvc.perform(post("/api/simulations/{cityId}/step", city.getId())
                        .header("Authorization", bearerFor(user)))
                .andExpect(status().isOk());

        String body = mockMvc.perform(post("/api/simulations/{cityId}/step", city.getId())
                        .header("Authorization", bearerFor(user)))
                .andExpect(status().isTooManyRequests())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("Too many simulation-mutation requests");
    }

    @Test
    void agentChatIsRateLimited() throws Exception {
        User user = persistUser("rate-limit-agent@example.com");
        City city = persistCity("Rate Agent", user);

        mockMvc.perform(post("/api/agent/cities/{cityId}/chat", city.getId())
                        .header("Authorization", bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentChatPayload("show snapshot"))))
                .andExpect(status().isOk());

        String body = mockMvc.perform(post("/api/agent/cities/{cityId}/chat", city.getId())
                        .header("Authorization", bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentChatPayload("show snapshot"))))
                .andExpect(status().isTooManyRequests())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("Too many agent-chat requests");
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("hash");
        user.setRoles(Set.of("ROLE_USER"));
        return userRepository.save(user);
    }

    private City persistCity(String name, User owner) {
        City city = new City();
        city.setName(name);
        city.setOwner(owner);
        return cityRepository.save(city);
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtService.generateAccessToken(user.getEmail());
    }

    private record CityPayload(String name) {
    }

    private record AgentChatPayload(String message) {
    }
}
