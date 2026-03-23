package eu.catlabs.humanaity.simulation.application;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.application.EventApplicationService;
import eu.catlabs.humanaity.event.application.EventDraft;
import eu.catlabs.humanaity.event.domain.EventType;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.simulation.domain.KnowledgeUnlock;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.KnowledgeUnlockRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeHouseRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeKnownPlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:knowledge-progression;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
class KnowledgeProgressionServiceTest {

    @Autowired
    private KnowledgeProgressionService knowledgeProgressionService;
    @Autowired
    private KnowledgeUnlockRepository knowledgeUnlockRepository;
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private TribeHouseRepository tribeHouseRepository;
    @Autowired
    private TribeKnownPlaceRepository tribeKnownPlaceRepository;

    @BeforeEach
    void cleanDatabase() {
        tribeKnownPlaceRepository.deleteAll();
        tribeHouseRepository.deleteAll();
        knowledgeUnlockRepository.deleteAll();
        eventRepository.deleteAll();
        cityRepository.deleteAll();
    }

    @Test
    void deterministicProgressionUnlocksDiscoveryToApplicationChain() {
        City city = new City();
        city.setName("KnowledgeCity");
        City savedCity = cityRepository.save(city);

        eventApplicationService.emitEventsAtTick(savedCity.getId(), 1L, List.of(
                new EventDraft(
                        EventType.DISCOVERY_UNLOCKED,
                        List.of(),
                        Map.of("trigger", "test"),
                        20,
                        "DISCOVERY:test:1"
                ),
                new EventDraft(
                        EventType.DIALOGUE_EXCHANGED,
                        List.of(),
                        Map.of("trigger", "test"),
                        20,
                        "DIALOGUE:test:1"
                )
        ));

        List<KnowledgeUnlock> created = knowledgeProgressionService.evaluateUnlocks(savedCity.getId(), 1L);
        List<KnowledgeUnlock> all = knowledgeUnlockRepository.findByCityIdOrderByUnlockedTickAscNodeIdAsc(savedCity.getId());
        List<String> unlockedIds = all.stream().map(KnowledgeUnlock::getNodeId).toList();

        assertThat(created).isNotEmpty();
        assertThat(unlockedIds).contains(
                "DISC_FIRE",
                "DISC_STORY",
                "INV_CAMPFIRE",
                "INV_ORAL_ARCHIVE",
                "APP_COOK_FOOD",
                "APP_TELL_STORIES"
        );

        List<KnowledgeUnlock> secondRun = knowledgeProgressionService.evaluateUnlocks(savedCity.getId(), 2L);
        assertThat(secondRun).isEmpty();
    }
}
