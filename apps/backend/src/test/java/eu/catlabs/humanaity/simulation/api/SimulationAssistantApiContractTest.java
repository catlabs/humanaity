package eu.catlabs.humanaity.simulation.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.ai.domain.AiCallContextType;
import eu.catlabs.humanaity.ai.domain.AiCallLog;
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
import eu.catlabs.humanaity.ai.infrastructure.persistence.AiCallLogRepository;
import eu.catlabs.humanaity.simulation.application.SimulationApplicationService;
import eu.catlabs.humanaity.simulation.application.tribe.TribeDecisionType;
import eu.catlabs.humanaity.simulation.domain.TribeDecisionSource;
import eu.catlabs.humanaity.simulation.domain.TribePlan;
import eu.catlabs.humanaity.simulation.domain.TribePlanStatus;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.HumanGoalRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.KnowledgeUnlockRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.SimulationRunRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeHouseRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeKnownPlaceRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribePlanRepository;
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
import java.time.Instant;

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
    private AiCallLogRepository aiCallLogRepository;
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
    @Autowired
    private TribePlanRepository tribePlanRepository;
    @Autowired
    private SimulationApplicationService simulationApplicationService;
    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        tribePlanRepository.deleteAll();
        tribeKnownPlaceRepository.deleteAll();
        tribeHouseRepository.deleteAll();
        knowledgeUnlockRepository.deleteAll();
        humanGoalRepository.deleteAll();
        inventionRepository.deleteAll();
        aiCallLogRepository.deleteAll();
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
    void assistantAllowsOtherAuthenticatedUsersOnSharedCities() throws Exception {
        User owner = persistUser("assistant-owner-shared@example.com");
        User other = persistUser("assistant-other-shared@example.com");
        City city = persistCity("Assistant Shared City", owner);

        mockMvc.perform(post("/api/simulations/{cityId}/assistant", city.getId())
                        .header("Authorization", bearerFor(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("world status")))
                .andExpect(status().isOk());
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
        assertThat(root).hasSize(7);
        assertThat(root.toString()).contains("si chef");
        assertThat(root.toString()).contains("inventions");
        assertThat(root.toString()).contains("world status");
        assertThat(root.toString()).contains("recent events");
        assertThat(root.toString()).contains("relationships");
        assertThat(root.toString()).contains("ai logs");
        assertThat(root.toString()).contains("ai stats");
    }

    @Test
    void assistantReturnsStableEmptyChiefPlanState() throws Exception {
        User owner = persistUser("assistant-owner-chief-empty@example.com");
        City city = persistCity("Chief Empty City", owner);
        Human tribeA = persistHuman(city, "Ari", 0.20, 0.20);
        tribeA.setTribeId("tribe-a");
        humanRepository.save(tribeA);
        Human tribeB = persistHuman(city, "Bo", 0.70, 0.70);
        tribeB.setTribeId("tribe-b");
        humanRepository.save(tribeB);

        MvcResult result = mockMvc.perform(post("/api/simulations/{cityId}/assistant", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("si chef")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.get("ok").asBoolean()).isTrue();
        assertThat(payload.get("commandType").asText()).isEqualTo("CHIEF_PLAN");
        assertThat(payload.get("text").asText()).contains("No chief plans");
        assertThat(payload.get("blocks").get(0).get("items")).hasSize(2);
        assertThat(payload.get("blocks").get(0).toString()).contains("No chief plan yet");
    }

    @Test
    void assistantReturnsPersistedChiefPlanState() throws Exception {
        User owner = persistUser("assistant-owner-chief-data@example.com");
        City city = persistCity("Chief Data City", owner);
        Human chief = persistHuman(city, "Ari", 0.20, 0.20);
        chief.setTribeId("tribe-a");
        chief = humanRepository.save(chief);
        Human member = persistHuman(city, "Bo", 0.28, 0.24);
        member.setTribeId("tribe-a");
        member = humanRepository.save(member);

        TribePlan plan = new TribePlan();
        plan.setCity(city);
        plan.setTribeId("tribe-a");
        plan.setChiefHumanId(chief.getId());
        plan.setPlanType(TribeDecisionType.GROUP_TRAVEL);
        plan.setPlanStatus(TribePlanStatus.ACTIVE);
        plan.setTargetPlaceId("forest");
        plan.setAssignedHumanIds(List.of(member.getId(), chief.getId()));
        plan.setDecisionSource(TribeDecisionSource.DETERMINISTIC);
        plan.setReasonSummary("Two tribe members travel together to forest");
        plan.setPlanMetadataKey("tribe-a:forest:10");
        plan.setLastAssignedTick(10L);
        tribePlanRepository.save(plan);

        MvcResult result = mockMvc.perform(post("/api/simulations/{cityId}/assistant", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("chief plan")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.get("commandType").asText()).isEqualTo("CHIEF_PLAN");
        JsonNode item = payload.get("blocks").get(0).get("items").get(0);
        assertThat(item.get("subtitle").asText()).contains("Chief Ari");
        assertThat(item.get("body").asText()).contains("Plan: Group Travel", "Status: Active", "Target place: Forest");
        assertThat(item.get("body").asText()).contains("Assigned humans: Bo, Ari");
        assertThat(item.get("body").asText()).contains("Decision source: Deterministic");
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
        assertThat(payload.get("blocks").get(0).get("items")).hasSize(7);
    }

    @Test
    void assistantReturnsAiStatsAndLogsBlocks() throws Exception {
        User owner = persistUser("assistant-owner-ai-observability@example.com");
        City city = persistCity("AI Observatory City", owner);

        aiCallLogRepository.save(aiLog(city, AiCallContextType.CHAT_FALLBACK, true, false, "OPENAI", "gpt-4", 12L, "Fallback prompt", "Fallback response", null, Instant.parse("2026-03-23T10:03:00Z")));
        aiCallLogRepository.save(aiLog(city, AiCallContextType.EVENT_ENRICHMENT, false, true, "OPENAI", "gpt-4", 24L, "Event prompt", null, "No JSON", Instant.parse("2026-03-23T10:02:00Z")));
        aiCallLogRepository.save(aiLog(city, AiCallContextType.CHIEF_DECISION, true, false, "ANTHROPIC", "claude-3", 36L, "Chief prompt", "Chief response", null, Instant.parse("2026-03-23T10:01:00Z")));

        MvcResult statsResult = mockMvc.perform(post("/api/simulations/{cityId}/assistant", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("ai stats")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode statsPayload = objectMapper.readTree(statsResult.getResponse().getContentAsString());
        assertThat(statsPayload.get("commandType").asText()).isEqualTo("AI_STATS");
        assertThat(statsPayload.get("blocks")).hasSize(4);
        assertThat(statsPayload.get("blocks").get(0).get("metrics").toString()).contains("Total", "3", "Fallback", "1");
        assertThat(statsPayload.get("blocks").get(1).get("items").toString()).contains("Chat Fallback", "Chief Decision", "Event Enrichment");
        assertThat(statsPayload.get("blocks").get(2).get("items").toString()).contains("OPENAI", "ANTHROPIC");
        assertThat(statsPayload.get("blocks").get(3).get("items").toString()).contains("gpt-4", "claude-3");

        MvcResult logsResult = mockMvc.perform(post("/api/simulations/{cityId}/assistant", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("ai logs")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode logsPayload = objectMapper.readTree(logsResult.getResponse().getContentAsString());
        assertThat(logsPayload.get("commandType").asText()).isEqualTo("AI_LOGS");
        JsonNode logItems = logsPayload.get("blocks").get(0).get("items");
        assertThat(logItems).hasSize(3);
        assertThat(logItems.get(0).get("chips").toString()).contains("Success", "OPENAI", "gpt-4");
        assertThat(logItems.toString()).contains("Prompt", "Result", "Fallback", "City");
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

    private AiCallLog aiLog(
            City city,
            AiCallContextType contextType,
            boolean success,
            boolean fallbackUsed,
            String provider,
            String model,
            long durationMs,
            String promptSummary,
            String responseSummary,
            String errorMessage,
            Instant requestedAt
    ) {
        AiCallLog log = new AiCallLog();
        log.setCity(city);
        log.setContextType(contextType);
        log.setContextEntityType("TEST");
        log.setContextEntityId(contextType.name());
        log.setProvider(provider);
        log.setModel(model);
        log.setSuccess(success);
        log.setFallbackUsed(fallbackUsed);
        log.setDurationMs(durationMs);
        log.setPromptSummary(promptSummary);
        log.setResponseSummary(responseSummary);
        log.setPromptHash("abc");
        log.setResponseHash(responseSummary == null ? null : "def");
        log.setErrorCode(errorMessage == null ? null : "TEST_ERROR");
        log.setErrorMessage(errorMessage);
        log.setRequestedAt(requestedAt);
        return aiCallLogRepository.save(log);
    }

    private record AssistantRequest(String commandText) {
    }
}
