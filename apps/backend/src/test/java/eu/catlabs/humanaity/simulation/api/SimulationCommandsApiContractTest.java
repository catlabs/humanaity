package eu.catlabs.humanaity.simulation.api;

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
import eu.catlabs.humanaity.simulation.infrastructure.persistence.HumanGoalRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.KnowledgeUnlockRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.SimulationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:simulation-commands-api;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
@AutoConfigureMockMvc
class SimulationCommandsApiContractTest {

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
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
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
    void commandsRejectUnauthenticatedCaller() throws Exception {
        User owner = persistUser("owner-command-unauth@example.com");
        City city = persistCity("Owned", owner);

        mockMvc.perform(post("/api/simulations/{cityId}/commands", city.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("advance 2")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void commandsRejectNonOwner() throws Exception {
        User owner = persistUser("owner-command-forbidden@example.com");
        User other = persistUser("other-command-forbidden@example.com");
        City city = persistCity("Owned", owner);

        mockMvc.perform(post("/api/simulations/{cityId}/commands", city.getId())
                        .header("Authorization", bearerFor(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("advance 2")))
                .andExpect(status().isForbidden());
    }

    @Test
    void advanceCommandExecutesDeterministically() throws Exception {
        User owner = persistUser("owner-command-advance@example.com");
        City city = persistCity("AdvanceCity", owner);

        MvcResult result = mockMvc.perform(post("/api/simulations/{cityId}/commands", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("advance 2")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.get("ok").asBoolean()).isTrue();
        assertThat(payload.get("commandType").asText()).isEqualTo("ADVANCE");
        assertThat(payload.get("mutated").asBoolean()).isTrue();
        assertThat(payload.get("message").asText()).contains("Advanced city by 2 steps");
        assertThat(payload.get("uiEffects").toString()).contains("REFRESH_SNAPSHOT", "REFRESH_TIMELINE");
    }

    @Test
    void focusCommandReturnsStableFocusEffect() throws Exception {
        User owner = persistUser("owner-command-focus@example.com");
        City city = persistCity("FocusCity", owner);
        Human human = persistHuman(city, "Ada", 0.3, 0.6);

        MvcResult result = mockMvc.perform(post("/api/simulations/{cityId}/commands", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("focus Ada")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.get("ok").asBoolean()).isTrue();
        assertThat(payload.get("commandType").asText()).isEqualTo("FOCUS_HUMAN");
        assertThat(payload.get("mutated").asBoolean()).isFalse();
        assertThat(payload.path("referencedEntities").path("humanId").asLong()).isEqualTo(human.getId());
        assertThat(payload.get("uiEffects").toString()).contains("FOCUS_HUMAN");
    }

    @Test
    void moveCommandAssignsGoalAndHighlightsPlace() throws Exception {
        User owner = persistUser("owner-command-move@example.com");
        City city = persistCity("MoveCity", owner);
        Human human = persistHuman(city, "Elsa", 0.9, 0.9);

        MvcResult result = mockMvc.perform(post("/api/simulations/{cityId}/commands", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("move Elsa forest")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.get("ok").asBoolean()).isTrue();
        assertThat(payload.get("commandType").asText()).isEqualTo("MOVE_HUMAN_TO_PLACE");
        assertThat(payload.get("mutated").asBoolean()).isTrue();
        assertThat(payload.get("message").asText()).contains("advanced city by 1 step");
        assertThat(payload.path("referencedEntities").path("humanId").asLong()).isEqualTo(human.getId());
        assertThat(payload.path("referencedEntities").path("placeId").asText()).isEqualTo("forest");
        assertThat(payload.get("uiEffects").toString()).contains("REFRESH_SNAPSHOT", "REFRESH_TIMELINE", "FOCUS_HUMAN", "HIGHLIGHT_PLACE");
        assertThat(humanGoalRepository.count()).isEqualTo(1);
    }

    @Test
    void meetCommandAssignsGoalAndReturnsTwoHumanReferences() throws Exception {
        User owner = persistUser("owner-command-meet@example.com");
        City city = persistCity("MeetCity", owner);
        Human actor = persistHuman(city, "Ada", 0.2, 0.3);
        Human target = persistHuman(city, "Ben", 0.8, 0.7);

        MvcResult result = mockMvc.perform(post("/api/simulations/{cityId}/commands", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("meet " + actor.getId() + " " + target.getId())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.get("ok").asBoolean()).isTrue();
        assertThat(payload.get("commandType").asText()).isEqualTo("MEET_HUMAN");
        assertThat(payload.get("mutated").asBoolean()).isTrue();
        assertThat(payload.get("message").asText()).contains("advanced city by 1 step");
        assertThat(payload.path("referencedEntities").path("humanId").asLong()).isEqualTo(actor.getId());
        assertThat(payload.path("referencedEntities").path("targetHumanId").asLong()).isEqualTo(target.getId());
        assertThat(payload.get("uiEffects").toString()).contains("REFRESH_SNAPSHOT", "REFRESH_TIMELINE", "FOCUS_HUMAN");
        assertThat(humanGoalRepository.count()).isEqualTo(1);
    }

    @Test
    void meetCommandRejectsSameHuman() throws Exception {
        User owner = persistUser("owner-command-meet-invalid@example.com");
        City city = persistCity("MeetInvalidCity", owner);
        Human actor = persistHuman(city, "Ada", 0.2, 0.3);

        MvcResult result = mockMvc.perform(post("/api/simulations/{cityId}/commands", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("meet " + actor.getId() + " " + actor.getId())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.get("ok").asBoolean()).isFalse();
        assertThat(payload.get("commandType").asText()).isEqualTo("UNSUPPORTED");
        assertThat(payload.get("message").asText()).contains("two distinct humans");
    }

    @Test
    void commandPathFailsClosedForLegacyPhrasing() throws Exception {
        User owner = persistUser("owner-command-invalid@example.com");
        City city = persistCity("InvalidCity", owner);

        MvcResult result = mockMvc.perform(post("/api/simulations/{cityId}/commands", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("advance by 3 steps")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.get("ok").asBoolean()).isFalse();
        assertThat(payload.get("commandType").asText()).isEqualTo("UNSUPPORTED");
        assertThat(payload.get("mutated").asBoolean()).isFalse();
        assertThat(payload.get("message").asText()).contains("Unsupported command");
        assertThat(payload.get("uiEffects").isEmpty()).isTrue();
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtService.generateAccessToken(user.getEmail());
    }

    private String request(String commandText) throws Exception {
        return objectMapper.writeValueAsString(new CommandRequest(commandText));
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

    private record CommandRequest(String commandText) {
    }
}
