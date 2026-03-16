package eu.catlabs.humanaity.agent.application;

import eu.catlabs.humanaity.agent.api.dto.AgentChatRequestInput;
import eu.catlabs.humanaity.agent.api.dto.AgentChatResponseOutput;
import eu.catlabs.humanaity.ai.application.AiGenerationService;
import eu.catlabs.humanaity.ai.domain.AiResponse;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.simulation.application.SimulationPlaceRegistry;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.SimulationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:agent-chat-fallback;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
@AutoConfigureMockMvc
class AgentChatOrchestrationServiceFallbackTest {

    @Autowired
    private AgentChatOrchestrationService orchestrationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private HumanRepository humanRepository;
    @Autowired
    private SimulationRunRepository simulationRunRepository;
    @MockBean
    private AiGenerationService aiGenerationService;

    @BeforeEach
    void cleanDatabase() {
        simulationRunRepository.deleteAll();
        humanRepository.deleteAll();
        cityRepository.deleteAll();
        userRepository.deleteAll();
        Mockito.reset(aiGenerationService);
    }

    @Test
    void deterministicMatchWinsBeforeFallbackIsTried() {
        User owner = persistUser("owner-fallback-precedence@example.com");
        City city = persistCity(owner);

        AgentChatResponseOutput response = orchestrationService.orchestrate(city.getId(), owner, request("step 5"));

        assertThat(response.getExecutedActions()).hasSize(1);
        assertThat(response.getExecutedActions().get(0).getType()).isEqualTo("STEP_SIMULATION");
        verify(aiGenerationService, never()).generate(any());
    }

    @Test
    void fallbackCanResolveAmbiguousMoveCommand() {
        User owner = persistUser("owner-fallback-valid@example.com");
        City city = persistCity(owner);
        Human elsa = persistHuman(city, "Elsa");

        when(aiGenerationService.generate(any())).thenReturn(AiResponse.builder()
                .rawContent("""
                        {"type":"MOVE_TO_PLACE","primaryHumanName":"Elsa","secondaryHumanName":null,"placeId":"forest"}
                        """)
                .build());

        AgentChatResponseOutput response = orchestrationService.orchestrate(
                city.getId(),
                owner,
                request("Ask Elsa to go where people are gathering")
        );

        Human reloaded = humanRepository.findById(elsa.getId()).orElseThrow();
        SimulationPlaceRegistry.SimulationPlace forest = SimulationPlaceRegistry.byId("forest").orElseThrow();

        assertThat(response.getExecutedActions().get(0).getType()).isEqualTo("MOVE_HUMAN_TO_PLACE");
        assertThat(response.getExecutedActions().get(0).getStatus()).isEqualTo("COMPLETED");
        assertThat(reloaded.getX()).isEqualTo(forest.x());
        assertThat(reloaded.getY()).isEqualTo(forest.y());
        verify(aiGenerationService).generate(any());
    }

    @Test
    void invalidFallbackOutputFailsClosedWithoutMutation() {
        User owner = persistUser("owner-fallback-invalid@example.com");
        City city = persistCity(owner);
        Human elsa = persistHuman(city, "Elsa");
        double startX = elsa.getX();
        double startY = elsa.getY();

        when(aiGenerationService.generate(any())).thenReturn(AiResponse.builder()
                .rawContent("""
                        {"type":"MOVE_TO_PLACE","primaryHumanName":"Nobody","secondaryHumanName":null,"placeId":"forest"}
                        """)
                .build());

        AgentChatResponseOutput response = orchestrationService.orchestrate(
                city.getId(),
                owner,
                request("Ask Elsa to go where people are gathering")
        );

        Human reloaded = humanRepository.findById(elsa.getId()).orElseThrow();

        assertThat(response.getExecutedActions().get(0).getType()).isEqualTo("UNSUPPORTED_REQUEST");
        assertThat(response.getExecutedActions().get(0).getStatus()).isEqualTo("REJECTED");
        assertThat(response.getMessage()).contains("could not validate");
        assertThat(reloaded.getX()).isEqualTo(startX);
        assertThat(reloaded.getY()).isEqualTo(startY);
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
        user.setRoles(java.util.Set.of("ROLE_USER"));
        return userRepository.save(user);
    }

    private City persistCity(User owner) {
        City city = new City();
        city.setName("Fallback City");
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
}
