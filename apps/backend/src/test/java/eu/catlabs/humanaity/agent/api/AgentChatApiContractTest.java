package eu.catlabs.humanaity.agent.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import eu.catlabs.humanaity.auth.infrastructure.security.JwtService;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.invention.infrastructure.persistence.InventionRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.SimulationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:agent-chat-api;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
@AutoConfigureMockMvc
class AgentChatApiContractTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private SimulationRunRepository simulationRunRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private InventionRepository inventionRepository;
    @Autowired
    private HumanRepository humanRepository;
    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        inventionRepository.deleteAll();
        eventRepository.deleteAll();
        simulationRunRepository.deleteAll();
        humanRepository.deleteAll();
        cityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void chatRejectsUnauthenticatedCaller() throws Exception {
        User owner = persistUser("owner-agent-unauth@example.com");
        City city = persistCity("UnauthorizedChat", owner);

        mockMvc.perform(post("/api/agent/cities/{cityId}/chat", city.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("advance by 3 steps")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chatRejectsNonOwner() throws Exception {
        User owner = persistUser("owner-agent-forbidden@example.com");
        User other = persistUser("other-agent-forbidden@example.com");
        City city = persistCity("OwnedCity", owner);

        mockMvc.perform(post("/api/agent/cities/{cityId}/chat", city.getId())
                        .header("Authorization", bearerFor(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("show snapshot")))
                .andExpect(status().isForbidden());
    }

    @Test
    void chatReturnsUiFacingSkeletonPayloadForOwner() throws Exception {
        User owner = persistUser("owner-agent-ok@example.com");
        City city = persistCity("OwnedCity", owner);

        MvcResult result = mockMvc.perform(post("/api/agent/cities/{cityId}/chat", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("advance city by 2 steps")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.get("commandClass").asText()).isEqualTo("SAFE_MVP");
        assertThat(payload.get("message").asText()).contains("Advanced the city");
        assertThat(payload.get("conversationId").asText()).isNotBlank();
        assertThat(payload.get("referencedEntities").get("cityId").asLong()).isEqualTo(city.getId());
        assertThat(payload.get("executedActions").isArray()).isTrue();
        assertThat(payload.get("executedActions").size()).isEqualTo(1);
        assertThat(payload.get("executedActions").get(0).get("type").asText()).isEqualTo("STEP_SIMULATION");
        assertThat(payload.get("executedActions").get(0).get("status").asText()).isEqualTo("COMPLETED");
        assertThat(payload.get("uiEffects").isArray()).isTrue();
        assertThat(payload.get("uiEffects").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void chatRejectsEmptyMessageAsBadRequest() throws Exception {
        User owner = persistUser("owner-agent-bad-request@example.com");
        City city = persistCity("OwnedCity", owner);

        mockMvc.perform(post("/api/agent/cities/{cityId}/chat", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("  ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatRejectsOutOfScopeRequestWithinSafeBoundaryMessage() throws Exception {
        User owner = persistUser("owner-agent-unsupported@example.com");
        City city = persistCity("OwnedCity", owner);

        MvcResult result = mockMvc.perform(post("/api/agent/cities/{cityId}/chat", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("delete the whole city forever")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.get("executedActions").get(0).get("type").asText()).isEqualTo("UNSUPPORTED_REQUEST");
        assertThat(payload.get("executedActions").get(0).get("status").asText()).isEqualTo("REJECTED");
        assertThat(payload.get("message").asText()).contains("safe Sprint 8 commands");
    }

    @Test
    void chatSupportsGuidedFocusCommandWithStableEffectAndStructuredData() throws Exception {
        User owner = persistUser("owner-agent-guided-focus@example.com");
        City city = persistCity("GuidedCity", owner);
        Human target = persistHuman(city, "Mira", 0.3, 0.6);

        MvcResult result = mockMvc.perform(post("/api/agent/cities/{cityId}/chat", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("focus human " + target.getId())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.get("commandClass").asText()).isEqualTo("GUIDED");
        assertThat(payload.get("executedActions").get(0).get("type").asText()).isEqualTo("FOCUS_HUMAN");
        assertThat(payload.get("uiEffects").get(0).get("type").asText()).isEqualTo("FOCUS_HUMAN");
        assertThat(payload.get("uiEffects").get(0).get("humanId").asLong()).isEqualTo(target.getId());
        assertThat(payload.get("structuredData").has("focusHuman")).isTrue();
    }

    @Test
    void chatSupportsGuidedCompareCommandWithStructuredPairOutput() throws Exception {
        User owner = persistUser("owner-agent-guided-compare@example.com");
        City city = persistCity("GuidedCity", owner);
        Human left = persistHuman(city, "Ada", 0.2, 0.4);
        Human right = persistHuman(city, "Ben", 0.8, 0.7);

        MvcResult result = mockMvc.perform(post("/api/agent/cities/{cityId}/chat", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("compare humans " + left.getId() + " and " + right.getId())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.get("commandClass").asText()).isEqualTo("GUIDED");
        assertThat(payload.get("executedActions").get(0).get("type").asText()).isEqualTo("COMPARE_HUMANS");
        assertThat(payload.get("referencedEntities").get("humanIds").size()).isEqualTo(2);
        assertThat(payload.get("structuredData").has("compareHumans")).isTrue();
        assertThat(payload.get("structuredData").get("compareHumans").has("left")).isTrue();
        assertThat(payload.get("structuredData").get("compareHumans").has("right")).isTrue();
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

    private Human persistHuman(City city, String name, double x, double y) {
        Human human = new Human();
        human.setCity(city);
        human.setName(name);
        human.setBusy(false);
        human.setX(x);
        human.setY(y);
        return humanRepository.save(human);
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtService.generateAccessToken(user.getEmail());
    }

    private String request(String message) throws Exception {
        return objectMapper.writeValueAsString(new AgentChatRequestPayload(message, null, null, null, null));
    }

    private record AgentChatRequestPayload(
            String message,
            String conversationId,
            Long selectedHumanId,
            Long selectedEventId,
            Long selectedInventionId
    ) {
    }
}
