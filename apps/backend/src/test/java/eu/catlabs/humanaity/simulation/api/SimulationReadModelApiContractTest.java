package eu.catlabs.humanaity.simulation.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.invention.infrastructure.persistence.InventionRepository;
import eu.catlabs.humanaity.simulation.application.SimulationApplicationService;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.KnowledgeUnlockRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.SimulationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:read-model-api;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
@AutoConfigureMockMvc(addFilters = false)
class SimulationReadModelApiContractTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SimulationApplicationService simulationApplicationService;
    @Autowired
    private InventionRepository inventionRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private SimulationRunRepository simulationRunRepository;
    @Autowired
    private HumanRepository humanRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private KnowledgeUnlockRepository knowledgeUnlockRepository;

    @BeforeEach
    void cleanDatabase() {
        knowledgeUnlockRepository.deleteAll();
        inventionRepository.deleteAll();
        eventRepository.deleteAll();
        simulationRunRepository.deleteAll();
        humanRepository.deleteAll();
        cityRepository.deleteAll();
    }

    @Test
    void overviewEndpointReturnsBackendOwnedContractForMixedRunStates() throws Exception {
        City noRunCity = createCityWithHumans("NoRun", List.of(
                new HumanSeed("Ana", 0.20, 0.20),
                new HumanSeed("Ben", 0.25, 0.24)
        ));
        City withRunCity = createCityWithHumans("WithRun", List.of(
                new HumanSeed("Cyd", 0.40, 0.40),
                new HumanSeed("Dan", 0.41, 0.41)
        ));

        simulationApplicationService.createRun(withRunCity.getId(), 4242L);
        simulationApplicationService.step(withRunCity.getId(), 12);

        MvcResult result = mockMvc.perform(get("/api/simulations/overview"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.isArray()).isTrue();
        assertThat(payload.size()).isEqualTo(2);

        JsonNode noRunOverview = findOverviewByCityId(payload, noRunCity.getId());
        assertThat(noRunOverview.get("cityName").asText()).isEqualTo("NoRun");
        assertThat(noRunOverview.get("hasRun").asBoolean()).isFalse();
        assertThat(noRunOverview.get("runStatus").isNull()).isTrue();
        assertThat(noRunOverview.get("running").asBoolean()).isFalse();
        assertThat(noRunOverview.get("tick").asLong()).isEqualTo(0L);
        assertThat(noRunOverview.get("year").asInt()).isEqualTo(1);
        assertThat(noRunOverview.get("era").asText()).isEqualTo("FOUNDING");
        assertThat(noRunOverview.get("population").asInt()).isEqualTo(2);
        assertThat(noRunOverview.get("eventCount").asInt()).isEqualTo(0);
        assertThat(noRunOverview.get("inventionCount").asInt()).isEqualTo(0);
        assertThat(noRunOverview.get("discoveryUnlockCount").asInt()).isEqualTo(0);
        assertThat(noRunOverview.get("unlockedInventionCount").asInt()).isEqualTo(0);
        assertThat(noRunOverview.get("applicationUnlockCount").asInt()).isEqualTo(0);

        JsonNode withRunOverview = findOverviewByCityId(payload, withRunCity.getId());
        assertThat(withRunOverview.get("cityName").asText()).isEqualTo("WithRun");
        assertThat(withRunOverview.get("hasRun").asBoolean()).isTrue();
        assertThat(withRunOverview.get("runStatus").asText()).isNotBlank();
        assertThat(withRunOverview.get("running").asBoolean()).isFalse();
        assertThat(withRunOverview.get("tick").asLong()).isEqualTo(12L);
        assertThat(withRunOverview.get("year").asInt()).isEqualTo(2);
        assertThat(withRunOverview.get("era").asText()).isEqualTo("FOUNDING");
        assertThat(withRunOverview.get("population").asInt()).isEqualTo(2);
        assertThat(withRunOverview.get("eventCount").asInt()).isEqualTo(eventRepository.findByCityIdOrderByTickAscSequenceInTickAscIdAsc(withRunCity.getId()).size());
        assertThat(withRunOverview.get("inventionCount").asInt()).isEqualTo(inventionRepository.findByCityIdOrderByTickCreatedAscInventionKeyAscIdAsc(withRunCity.getId()).size());
        assertThat(withRunOverview.has("discoveryUnlockCount")).isTrue();
        assertThat(withRunOverview.has("unlockedInventionCount")).isTrue();
        assertThat(withRunOverview.has("applicationUnlockCount")).isTrue();
    }

    @Test
    void snapshotEndpointReturnsExplicitNoRunYetContract() throws Exception {
        City city = createCityWithHumans("SnapshotNoRun", List.of(
                new HumanSeed("Ari", 0.10, 0.15),
                new HumanSeed("Bo", 0.15, 0.20),
                new HumanSeed("Cy", 0.20, 0.25)
        ));

        MvcResult result = mockMvc.perform(get("/api/simulations/{cityId}/snapshot", city.getId()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode snapshot = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(snapshot.get("city").get("id").asLong()).isEqualTo(city.getId());
        assertThat(snapshot.get("run").get("hasRun").asBoolean()).isFalse();
        assertThat(snapshot.get("run").get("runId").isNull()).isTrue();
        assertThat(snapshot.get("run").get("seed").isNull()).isTrue();
        assertThat(snapshot.get("run").get("status").isNull()).isTrue();
        assertThat(snapshot.get("run").get("running").asBoolean()).isFalse();
        assertThat(snapshot.get("run").get("tick").asLong()).isEqualTo(0L);
        assertThat(snapshot.get("run").get("year").asInt()).isEqualTo(1);
        assertThat(snapshot.get("run").get("era").asText()).isEqualTo("FOUNDING");

        JsonNode humans = snapshot.get("humans");
        assertThat(humans.isArray()).isTrue();
        assertThat(humans.size()).isEqualTo(3);
        assertSortedAscendingById(humans);

        JsonNode metrics = snapshot.get("metrics");
        assertThat(metrics.get("population").asInt()).isEqualTo(3);
        assertThat(metrics.get("eventCount").asInt()).isEqualTo(0);
        assertThat(metrics.get("inventionCount").asInt()).isEqualTo(0);

        JsonNode timelineSummary = snapshot.get("timelineSummary");
        assertThat(timelineSummary.get("latestEventTick").isNull()).isTrue();
        assertThat(timelineSummary.get("latestInventionTick").isNull()).isTrue();
        assertThat(timelineSummary.get("latestKnowledgeUnlockTick").isNull()).isTrue();
        assertThat(timelineSummary.get("recentEventCount").asInt()).isEqualTo(0);
        assertThat(timelineSummary.get("recentInventionCount").asInt()).isEqualTo(0);
        assertThat(timelineSummary.get("recentKnowledgeUnlockCount").asInt()).isEqualTo(0);
        assertThat(snapshot.get("knowledge").isObject()).isTrue();
        assertThat(snapshot.get("knowledge").get("unlockedDiscoveries").isArray()).isTrue();
        assertThat(snapshot.get("knowledge").get("unlockedInventions").isArray()).isTrue();
        assertThat(snapshot.get("knowledge").get("unlockedApplications").isArray()).isTrue();

        assertThat(snapshot.get("recentEvents").isArray()).isTrue();
        assertThat(snapshot.get("recentEvents").size()).isEqualTo(0);
        assertThat(snapshot.get("recentInventions").isArray()).isTrue();
        assertThat(snapshot.get("recentInventions").size()).isEqualTo(0);
    }

    @Test
    void snapshotEndpointReturnsExplicitNoHistoryYetContractWhenRunExists() throws Exception {
        City city = createCityWithHumans("SnapshotRunNoHistory", List.of(
                new HumanSeed("Dex", 0.60, 0.60),
                new HumanSeed("Ena", 0.70, 0.70)
        ));
        simulationApplicationService.createRun(city.getId(), 999L);

        MvcResult result = mockMvc.perform(get("/api/simulations/{cityId}/snapshot", city.getId()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode snapshot = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(snapshot.get("run").get("hasRun").asBoolean()).isTrue();
        assertThat(snapshot.get("run").get("runId").isNull()).isFalse();
        assertThat(snapshot.get("run").get("seed").asLong()).isEqualTo(999L);
        assertThat(snapshot.get("run").get("tick").asLong()).isEqualTo(0L);
        assertThat(snapshot.get("run").get("year").asInt()).isEqualTo(1);
        assertThat(snapshot.get("run").get("era").asText()).isEqualTo("FOUNDING");
        assertThat(snapshot.get("run").get("status").asText()).isEqualTo("CREATED");
        assertThat(snapshot.get("run").get("running").asBoolean()).isFalse();

        assertThat(snapshot.get("recentEvents").isArray()).isTrue();
        assertThat(snapshot.get("recentEvents").size()).isEqualTo(0);
        assertThat(snapshot.get("recentInventions").isArray()).isTrue();
        assertThat(snapshot.get("recentInventions").size()).isEqualTo(0);
        assertThat(snapshot.get("timelineSummary").get("latestEventTick").isNull()).isTrue();
        assertThat(snapshot.get("timelineSummary").get("latestInventionTick").isNull()).isTrue();
    }

    @Test
    void snapshotEndpointReturnsNotFoundForUnknownCity() throws Exception {
        mockMvc.perform(get("/api/simulations/{cityId}/snapshot", 999_999L))
                .andExpect(status().isNotFound());
    }

    private City createCityWithHumans(String name, List<HumanSeed> seeds) {
        City city = new City();
        city.setName(name);
        City savedCity = cityRepository.save(city);

        for (HumanSeed seed : seeds) {
            humanRepository.save(createHuman(savedCity, seed.name(), seed.x(), seed.y()));
        }
        return savedCity;
    }

    private Human createHuman(City city, String name, double x, double y) {
        Human human = new Human();
        human.setCity(city);
        human.setName(name);
        human.setBusy(false);
        human.setX(x);
        human.setY(y);
        return human;
    }

    private JsonNode findOverviewByCityId(JsonNode payload, Long cityId) {
        return StreamSupport.stream(payload.spliterator(), false)
                .filter(node -> node.get("cityId").asLong() == cityId)
                .findFirst()
                .orElseThrow();
    }

    private void assertSortedAscendingById(JsonNode humans) {
        List<Long> ids = StreamSupport.stream(humans.spliterator(), false)
                .map(node -> node.get("id").asLong())
                .toList();
        List<Long> sorted = ids.stream().sorted(Comparator.naturalOrder()).toList();
        assertThat(ids).isEqualTo(sorted);
    }

    private record HumanSeed(
            String name,
            double x,
            double y
    ) {
    }
}
