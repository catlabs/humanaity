package eu.catlabs.humanaity.ai.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.agent.api.dto.AgentChatRequestInput;
import eu.catlabs.humanaity.agent.application.AgentChatOrchestrationService;
import eu.catlabs.humanaity.ai.application.AiGenerationContext;
import eu.catlabs.humanaity.ai.application.AiGenerationService;
import eu.catlabs.humanaity.ai.application.enrichment.AiHistoryEnrichmentService;
import eu.catlabs.humanaity.ai.domain.AiCallContextType;
import eu.catlabs.humanaity.ai.domain.AiCallLog;
import eu.catlabs.humanaity.ai.domain.AiProvider;
import eu.catlabs.humanaity.ai.domain.AiResponse;
import eu.catlabs.humanaity.ai.infrastructure.persistence.AiCallLogRepository;
import eu.catlabs.humanaity.ai.infrastructure.port.AiProviderPort;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import eu.catlabs.humanaity.auth.infrastructure.security.JwtService;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.domain.EventCategory;
import eu.catlabs.humanaity.event.domain.EventType;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.history.domain.HistoryEra;
import eu.catlabs.humanaity.invention.domain.Invention;
import eu.catlabs.humanaity.invention.domain.InventionCategory;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.HumanGoalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ai-observability-api;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
@AutoConfigureMockMvc
class AiObservabilityApiContractTest {

    @Autowired
    private AgentChatOrchestrationService orchestrationService;
    @Autowired
    private AiGenerationService aiGenerationService;
    @Autowired
    private AiHistoryEnrichmentService aiHistoryEnrichmentService;
    @Autowired
    private AiCallLogRepository aiCallLogRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private HumanRepository humanRepository;
    @Autowired
    private HumanGoalRepository humanGoalRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiProviderPort aiProviderPort;

    @BeforeEach
    void cleanDatabase() {
        aiCallLogRepository.deleteAll();
        humanGoalRepository.deleteAll();
        humanRepository.deleteAll();
        cityRepository.deleteAll();
        userRepository.deleteAll();
        when(aiProviderPort.isAvailable()).thenReturn(true);
        when(aiProviderPort.getProviderType()).thenReturn(AiProvider.OPENAI);
    }

    @Test
    void writesAiLogForFallbackInterpretation() {
        when(aiProviderPort.generate(any())).thenReturn(AiResponse.builder()
                .rawContent("""
                        {"type":"MOVE_TO_PLACE","primaryHumanName":"Elsa","secondaryHumanName":null,"placeId":"forest"}
                        """)
                .provider(AiProvider.OPENAI)
                .model("test-fallback-model")
                .build());

        User owner = persistUser("ai-fallback-owner@example.com");
        City city = persistCity(owner);
        persistHuman(city, "Elsa");

        orchestrationService.orchestrate(city.getId(), owner, request("Ask Elsa to go where people are gathering"));

        List<AiCallLog> logs = aiCallLogRepository.findAll();
        assertThat(logs).hasSize(1);
        AiCallLog log = logs.get(0);
        assertThat(log.getContextType()).isEqualTo(AiCallContextType.CHAT_FALLBACK);
        assertThat(log.getCity().getId()).isEqualTo(city.getId());
        assertThat(log.isSuccess()).isTrue();
        assertThat(log.isFallbackUsed()).isFalse();
        assertThat(log.getModel()).isEqualTo("test-fallback-model");
    }

    @Test
    void writesAiLogsForEventAndInventionEnrichment() {
        when(aiProviderPort.generate(any()))
                .thenReturn(AiResponse.builder()
                        .rawContent("{\"title\":\"Shared Loom\",\"summary\":\"A stronger weaving method.\"}")
                        .provider(AiProvider.OPENAI)
                        .model("test-enrichment-model")
                        .build())
                .thenReturn(AiResponse.builder()
                        .rawContent("{\"snippet\":\"Two neighbors exchanged methods for grain storage.\"}")
                        .provider(AiProvider.OPENAI)
                        .model("test-enrichment-model")
                        .build());

        User owner = persistUser("ai-enrichment-owner@example.com");
        City city = persistCity(owner);

        Invention invention = new Invention();
        invention.setCity(city);
        invention.setTickCreated(4L);
        invention.setCategory(InventionCategory.KNOWLEDGE);
        invention.setInventionKey("INV-LOOM");
        invention.setTitle("Loom");
        invention.setSummary("Threads are organized into a weaving frame.");
        invention.setImpactScore(55);
        invention.setYearCreated(3);
        invention.setEraCreated(HistoryEra.FOUNDING);

        Event event = new Event();
        event.setCity(city);
        event.setEventType(EventType.DIALOGUE_EXCHANGED);
        event.setEventCategory(EventCategory.DIALOGUE);
        event.setEventKey("DIALOGUE:1");
        event.setTick(5L);
        event.setYear(3);
        event.setEra(HistoryEra.FOUNDING);
        event.setActorIds(List.of(1L, 2L));
        event.setPayload(Map.of("dialogueKey", "1-2-5"));
        event.setImportance(30);

        aiHistoryEnrichmentService.enrichInvention(invention);
        aiHistoryEnrichmentService.enrichEventDialogueIfEligible(event);

        List<AiCallLog> logs = aiCallLogRepository.findAll();
        assertThat(logs).hasSize(2);
        assertThat(logs).extracting(AiCallLog::getContextType)
                .containsExactlyInAnyOrder(AiCallContextType.INVENTION_ENRICHMENT, AiCallContextType.EVENT_ENRICHMENT);
        assertThat(logs).allMatch(AiCallLog::isSuccess);
        assertThat(logs).allMatch(log -> !log.isFallbackUsed());
    }

    @Test
    void aiGenerationServiceStoresResponseSummaryAndContextMetadata() {
        when(aiProviderPort.generate(any())).thenReturn(AiResponse.builder()
                .rawContent("{\"answer\":\"Observed\"}")
                .provider(AiProvider.OPENAI)
                .model("test-observability-model")
                .build());

        User owner = persistUser("ai-generation-owner@example.com");
        City city = persistCity(owner);

        aiGenerationService.generate(
                eu.catlabs.humanaity.ai.domain.AiPrompt.builder()
                        .systemMessage("System")
                        .userMessage("User")
                        .build(),
                new AiGenerationContext(
                        AiCallContextType.CHIEF_DECISION,
                        city.getId(),
                        "TRIBE_DECISION",
                        "tribe-a",
                        "Choose a tribe decision candidate.",
                        true
                )
        );

        List<AiCallLog> logs = aiCallLogRepository.findAll();
        assertThat(logs).hasSize(1);
        AiCallLog log = logs.get(0);
        assertThat(log.getContextType()).isEqualTo(AiCallContextType.CHIEF_DECISION);
        assertThat(log.getCity().getId()).isEqualTo(city.getId());
        assertThat(log.getResponseSummary()).contains("Observed");
        assertThat(log.getProvider()).isEqualTo("OPENAI");
        assertThat(log.getModel()).isEqualTo("test-observability-model");
    }

    @Test
    void summaryEndpointAggregatesCountsAndListEndpointFilters() throws Exception {
        User owner = persistUser("ai-summary-owner@example.com");
        City city = persistCity(owner);

        aiCallLogRepository.save(log(city, AiCallContextType.CHAT_FALLBACK, true, false, "gpt-fallback", null));
        aiCallLogRepository.save(log(city, AiCallContextType.EVENT_ENRICHMENT, false, true, "gpt-events", "Bad response"));
        aiCallLogRepository.save(log(city, AiCallContextType.INVENTION_ENRICHMENT, true, false, "gpt-inventions", null));

        MvcResult listResult = mockMvc.perform(get("/api/ai/logs")
                        .header("Authorization", bearerFor(owner))
                        .param("cityId", String.valueOf(city.getId()))
                        .param("contextType", "CHAT_FALLBACK"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listPayload = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertThat(listPayload).hasSize(1);
        assertThat(listPayload.get(0).get("contextType").asText()).isEqualTo("CHAT_FALLBACK");

        MvcResult summaryResult = mockMvc.perform(get("/api/ai/logs/summary")
                        .header("Authorization", bearerFor(owner))
                        .param("cityId", String.valueOf(city.getId())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode summary = objectMapper.readTree(summaryResult.getResponse().getContentAsString());
        assertThat(summary.get("totalCount").asLong()).isEqualTo(3L);
        assertThat(summary.get("successCount").asLong()).isEqualTo(2L);
        assertThat(summary.get("failureCount").asLong()).isEqualTo(1L);
        assertThat(summary.get("fallbackCount").asLong()).isEqualTo(1L);
        assertThat(summary.get("byContextType").toString()).contains("CHAT_FALLBACK", "EVENT_ENRICHMENT", "INVENTION_ENRICHMENT");
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtService.generateAccessToken(user.getEmail());
    }

    private AgentChatRequestInput request(String message) {
        AgentChatRequestInput input = new AgentChatRequestInput();
        input.setMessage(message);
        return input;
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("hashed-password");
        user.setRoles(Set.of("ROLE_USER"));
        return userRepository.save(user);
    }

    private City persistCity(User owner) {
        City city = new City();
        city.setName("AI Observability City");
        city.setOwner(owner);
        return cityRepository.save(city);
    }

    private Human persistHuman(City city, String name) {
        Human human = new Human();
        human.setCity(city);
        human.setName(name);
        human.setBusy(false);
        human.setX(0.25);
        human.setY(0.35);
        return humanRepository.save(human);
    }

    private AiCallLog log(City city, AiCallContextType contextType, boolean success, boolean fallbackUsed, String model, String errorMessage) {
        AiCallLog log = new AiCallLog();
        log.setCity(city);
        log.setContextType(contextType);
        log.setContextEntityType("TEST");
        log.setContextEntityId(contextType.name());
        log.setProvider("OPENAI");
        log.setModel(model);
        log.setSuccess(success);
        log.setFallbackUsed(fallbackUsed);
        log.setDurationMs(42L);
        log.setPromptSummary("Test summary");
        log.setPromptHash("abc");
        log.setResponseHash(success ? "def" : null);
        log.setErrorCode(errorMessage == null ? null : "TEST_ERROR");
        log.setErrorMessage(errorMessage);
        log.setRequestedAt(Instant.now());
        return log;
    }
}
