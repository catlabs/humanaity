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
import eu.catlabs.humanaity.simulation.infrastructure.persistence.HumanGoalRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.KnowledgeUnlockRepository;
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
        "spring.datasource.url=jdbc:h2:mem:history-api;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
@AutoConfigureMockMvc(addFilters = false)
class SimulationHistoryApiContractTest {

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
    @Autowired
    private HumanGoalRepository humanGoalRepository;

    @BeforeEach
    void cleanDatabase() {
        knowledgeUnlockRepository.deleteAll();
        humanGoalRepository.deleteAll();
        inventionRepository.deleteAll();
        eventRepository.deleteAll();
        simulationRunRepository.deleteAll();
        humanRepository.deleteAll();
        cityRepository.deleteAll();
    }

    @Test
    void cityScopedEventAndTimelineEndpointsExposeOrderedStablePayload() throws Exception {
        City cityA = createCityWithHumans("One");
        City cityB = createCityWithHumans("Two");

        simulationApplicationService.createRun(cityA.getId(), 4242L);
        simulationApplicationService.createRun(cityB.getId(), 4242L);
        simulationApplicationService.step(cityA.getId(), 90);
        simulationApplicationService.step(cityB.getId(), 20);

        MvcResult eventResult = mockMvc.perform(get("/api/simulations/{cityId}/history/events", cityA.getId())
                        .param("fromTick", "1")
                        .param("toTick", "90")
                        .param("limit", "500"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode events = objectMapper.readTree(eventResult.getResponse().getContentAsString());
        assertThat(events.isArray()).isTrue();
        assertThat(events.size()).isGreaterThan(0);
        assertOrderedByTickAndSequence(events);
        assertAllCityScoped(events, cityA.getId());
        assertContractFieldsPresent(events.get(0));

        MvcResult timelineResult = mockMvc.perform(get("/api/simulations/{cityId}/history/timeline", cityA.getId())
                        .param("fromTick", "1")
                        .param("limit", "500"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode timeline = objectMapper.readTree(timelineResult.getResponse().getContentAsString());
        assertThat(timeline.get("cityId").asLong()).isEqualTo(cityA.getId());
        assertThat(timeline.get("events").isArray()).isTrue();
        assertThat(timeline.get("inventions").isArray()).isTrue();
        assertThat(timeline.get("eventCount").asInt()).isEqualTo(timeline.get("events").size());
        assertThat(timeline.get("inventionCount").asInt()).isEqualTo(timeline.get("inventions").size());
        if (timeline.get("inventions").size() > 0) {
            JsonNode firstInvention = timeline.get("inventions").get(0);
            assertThat(firstInvention.has("enrichmentStatus")).isTrue();
            assertThat(firstInvention.has("enrichmentFallback")).isTrue();
        }

        JsonNode timelineEvents = timeline.get("events");
        assertOrderedByTickAndSequence(timelineEvents);
        assertAllCityScoped(timelineEvents, cityA.getId());
    }

    @Test
    void invalidTickRangeReturnsBadRequest() throws Exception {
        City city = createCityWithHumans("Range");
        simulationApplicationService.createRun(city.getId(), 5150L);
        simulationApplicationService.step(city.getId(), 5);

        mockMvc.perform(get("/api/simulations/{cityId}/history/events", city.getId())
                        .param("fromTick", "10")
                        .param("toTick", "2"))
                .andExpect(status().isBadRequest());
    }

    private City createCityWithHumans(String name) {
        City city = new City();
        city.setName(name);
        City savedCity = cityRepository.save(city);

        humanRepository.save(createHuman(savedCity, "Ari", 0.42, 0.40));
        humanRepository.save(createHuman(savedCity, "Bo", 0.44, 0.41));
        humanRepository.save(createHuman(savedCity, "Cy", 0.45, 0.43));
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

    private void assertOrderedByTickAndSequence(JsonNode events) {
        long previousTick = Long.MIN_VALUE;
        int previousSequence = Integer.MIN_VALUE;
        for (JsonNode event : events) {
            long tick = event.get("tick").asLong();
            int sequence = event.get("sequenceInTick").asInt();
            if (tick == previousTick) {
                assertThat(sequence).isGreaterThan(previousSequence);
            } else {
                assertThat(tick).isGreaterThanOrEqualTo(previousTick);
                previousSequence = Integer.MIN_VALUE;
            }
            previousTick = tick;
            previousSequence = sequence;
        }
    }

    private void assertAllCityScoped(JsonNode events, Long cityId) {
        for (JsonNode event : events) {
            assertThat(event.get("cityId").asLong()).isEqualTo(cityId);
        }
    }

    private void assertContractFieldsPresent(JsonNode event) {
        assertThat(event.hasNonNull("eventKey")).isTrue();
        assertThat(event.hasNonNull("eventType")).isTrue();
        assertThat(event.hasNonNull("eventCategory")).isTrue();
        assertThat(event.has("payload")).isTrue();
        assertThat(event.get("payload").isObject()).isTrue();
        assertThat(event.has("actorIds")).isTrue();
        assertThat(event.get("actorIds").isArray()).isTrue();
        assertThat(event.has("enrichmentStatus")).isTrue();
        assertThat(event.has("enrichmentFallback")).isTrue();
    }
}
