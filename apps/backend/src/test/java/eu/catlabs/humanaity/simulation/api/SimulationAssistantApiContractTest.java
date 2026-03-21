package eu.catlabs.humanaity.simulation.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.ai.domain.AiEnrichmentStatus;
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
import eu.catlabs.humanaity.simulation.application.SimulationApplicationService;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:simulation-assistant-api;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
@AutoConfigureMockMvc
class SimulationAssistantApiContractTest {

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
    private EventRepository eventRepository;
    @Autowired
    private InventionRepository inventionRepository;
    @Autowired
    private SimulationRunRepository simulationRunRepository;
    @Autowired
    private KnowledgeUnlockRepository knowledgeUnlockRepository;
    @Autowired
    private SimulationApplicationService simulationApplicationService;
    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        knowledgeUnlockRepository.deleteAll();
        inventionRepository.deleteAll();
        eventRepository.deleteAll();
        simulationRunRepository.deleteAll();
        humanRepository.deleteAll();
        cityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void assistantRejectsUnauthenticatedCaller() throws Exception {
        User owner = persistUser("assistant-owner-unauth@example.com");
        City city = persistCity("Assistant City", owner);

        mockMvc.perform(post("/api/simulations/{cityId}/assistant", city.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("world status")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void assistantCommandsListRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/simulations/assistant/commands"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void assistantCommandsListReturnsCanonicalCommands() throws Exception {
        User owner = persistUser("assistant-owner-catalog@example.com");

        MvcResult result = mockMvc.perform(get("/api/simulations/assistant/commands")
                        .header("Authorization", bearerFor(owner)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(root.isArray()).isTrue();
        assertThat(root).hasSize(4);
        assertThat(root.toString()).contains("inventions");
        assertThat(root.toString()).contains("world status");
        assertThat(root.toString()).contains("recent events");
        assertThat(root.toString()).contains("relationships");
    }

    @Test
    void assistantReturnsWorldStatusBlocks() throws Exception {
        User owner = persistUser("assistant-owner-world@example.com");
        City city = persistCity("Status City", owner);
        persistHuman(city, "Ada", 0.25, 0.35);
        persistHuman(city, "Ben", 0.55, 0.45);
        simulationApplicationService.createRun(city.getId(), 77L);
        simulationApplicationService.step(city.getId(), 2);

        MvcResult result = mockMvc.perform(post("/api/simulations/{cityId}/assistant", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("world status")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.get("ok").asBoolean()).isTrue();
        assertThat(payload.get("commandType").asText()).isEqualTo("WORLD_STATUS");
        assertThat(payload.get("text").asText()).contains("current global status");
        assertThat(payload.get("blocks").isArray()).isTrue();
        assertThat(payload.get("blocks").get(0).get("type").asText()).isEqualTo("WORLD_STATUS");
        assertThat(payload.get("blocks").get(0).get("metrics").isArray()).isTrue();
        assertThat(payload.get("blocks").get(0).get("items").get(0).get("title").asText()).isEqualTo("Status City");
    }

    @Test
    void assistantReturnsInventionsAndRelationshipBlocks() throws Exception {
        User owner = persistUser("assistant-owner-data@example.com");
        City city = persistCity("Data City", owner);
        Human ada = persistHuman(city, "Ada", 0.20, 0.20);
        Human ben = persistHuman(city, "Ben", 0.40, 0.40);
        Human cy = persistHuman(city, "Cy", 0.60, 0.60);
        simulationApplicationService.createRun(city.getId(), 55L);

        inventionRepository.save(createInvention(city, 3L, InventionCategory.KNOWLEDGE, "stars", "Star Charts", "A first map of the night sky.", 72, 1));
        inventionRepository.save(createInvention(city, 4L, InventionCategory.TECHNIQUE, "kiln", "Clay Kiln", "Controlled heat for stronger pottery.", 81, 1));

        eventRepository.save(createEvent(city, 4L, 0, EventType.DIALOGUE_EXCHANGED, List.of(ada.getId(), ben.getId()), Map.of("topic", "plans"), 40, 1));
        eventRepository.save(createEvent(city, 5L, 0, EventType.HUMANS_COLLIDED, List.of(ada.getId(), ben.getId()), Map.of("place", "river"), 30, 1));
        eventRepository.save(createEvent(city, 6L, 0, EventType.DIALOGUE_EXCHANGED, List.of(ben.getId(), cy.getId()), Map.of("topic", "trade"), 35, 1));

        MvcResult inventionsResult = mockMvc.perform(post("/api/simulations/{cityId}/assistant", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("inventions")))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode inventionsPayload = objectMapper.readTree(inventionsResult.getResponse().getContentAsString());
        assertThat(inventionsPayload.get("commandType").asText()).isEqualTo("INVENTIONS");
        assertThat(inventionsPayload.get("blocks").get(0).get("items")).hasSize(2);
        assertThat(inventionsPayload.get("blocks").get(0).get("items").get(0).get("title").asText()).isEqualTo("Clay Kiln");

        MvcResult relationshipsResult = mockMvc.perform(post("/api/simulations/{cityId}/assistant", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("relationships")))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode relationshipsPayload = objectMapper.readTree(relationshipsResult.getResponse().getContentAsString());
        assertThat(relationshipsPayload.get("commandType").asText()).isEqualTo("RELATIONSHIPS");
        JsonNode relationshipItems = relationshipsPayload.get("blocks").get(0).get("items");
        assertThat(relationshipItems).hasSize(2);
        JsonNode adaBenItem = null;
        for (JsonNode item : relationshipItems) {
            String title = item.get("title").asText();
            if (title.contains("Ada") && title.contains("Ben")) {
                adaBenItem = item;
                break;
            }
        }
        assertThat(adaBenItem).isNotNull();
        assertThat(adaBenItem.get("body").asText()).contains("dialogues", "collisions");
    }

    @Test
    void assistantFailsClosedForUnsupportedCommand() throws Exception {
        User owner = persistUser("assistant-owner-unsupported@example.com");
        City city = persistCity("Unsupported City", owner);

        MvcResult result = mockMvc.perform(post("/api/simulations/{cityId}/assistant", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("tell me everything")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.get("ok").asBoolean()).isFalse();
        assertThat(payload.get("commandType").asText()).isEqualTo("UNSUPPORTED");
        assertThat(payload.get("blocks").get(0).get("items")).hasSize(4);
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtService.generateAccessToken(user.getEmail());
    }

    private String request(String commandText) throws Exception {
        return objectMapper.writeValueAsString(new AssistantRequest(commandText));
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

    private Invention createInvention(
            City city,
            long tickCreated,
            InventionCategory category,
            String inventionKey,
            String title,
            String summary,
            int impactScore,
            int yearCreated
    ) {
        Invention invention = new Invention();
        invention.setCity(city);
        invention.setTickCreated(tickCreated);
        invention.setCategory(category);
        invention.setInventionKey(inventionKey);
        invention.setTitle(title);
        invention.setSummary(summary);
        invention.setSourceEventKeys(List.of());
        invention.setImpactScore(impactScore);
        invention.setYearCreated(yearCreated);
        invention.setEraCreated(HistoryEra.FOUNDING);
        invention.setEnrichmentStatus(AiEnrichmentStatus.NONE);
        invention.setEnrichmentFallback(false);
        return invention;
    }

    private Event createEvent(
            City city,
            long tick,
            int sequenceInTick,
            EventType eventType,
            List<Long> actorIds,
            Map<String, String> payload,
            int importance,
            int year
    ) {
        Event event = new Event();
        event.setCity(city);
        event.setTick(tick);
        event.setSequenceInTick(sequenceInTick);
        event.setEventCategory(eventType.getCategory());
        event.setEventType(eventType);
        event.setActorIds(actorIds);
        event.setPayload(payload);
        event.setImportance(importance);
        event.setYear(year);
        event.setEra(HistoryEra.FOUNDING);
        event.setEventKey(eventType.name() + ":" + tick + ":" + sequenceInTick + ":" + actorIds.hashCode());
        event.setEnrichmentStatus(AiEnrichmentStatus.NONE);
        event.setEnrichmentFallback(false);
        return event;
    }

    private record AssistantRequest(String commandText) {
    }
}
