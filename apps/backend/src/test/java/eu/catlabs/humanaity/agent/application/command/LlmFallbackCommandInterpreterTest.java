package eu.catlabs.humanaity.agent.application.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.ai.application.AiGenerationContext;
import eu.catlabs.humanaity.ai.application.AiGenerationService;
import eu.catlabs.humanaity.ai.domain.AiResponse;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.human.domain.Human;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmFallbackCommandInterpreterTest {

    private final AiGenerationService aiGenerationService = mock(AiGenerationService.class);
    private final LlmFallbackCommandInterpreter interpreter =
            new LlmFallbackCommandInterpreter(aiGenerationService, new ObjectMapper());

    @Test
    void returnsValidatedMoveCommandWhenFallbackJsonIsValid() {
        when(aiGenerationService.generate(any(), any(AiGenerationContext.class))).thenReturn(AiResponse.builder()
                .rawContent("""
                        {"type":"MOVE_TO_PLACE","primaryHumanName":"Elsa","secondaryHumanName":null,"placeId":"forest"}
                        """)
                .build());

        FallbackCommandMatch match = interpreter.interpret(
                "ask elsa to go where people are gathering",
                List.of(human(1L, "Elsa"), human(2L, "Lucas"))
        );

        assertThat(match.status()).isEqualTo(FallbackCommandMatchStatus.MATCHED);
        assertThat(match.command().type()).isEqualTo(AgentChatCommandType.MOVE_TO_PLACE);
        assertThat(match.command().primaryHumanId()).isEqualTo(1L);
        assertThat(match.command().placeId()).isEqualTo("forest");
    }

    @Test
    void refusesInvalidFallbackOutput() {
        when(aiGenerationService.generate(any(), any(AiGenerationContext.class))).thenReturn(AiResponse.builder()
                .rawContent("""
                        {"type":"MOVE_TO_PLACE","primaryHumanName":"Unknown","secondaryHumanName":null,"placeId":"forest"}
                        """)
                .build());

        FallbackCommandMatch match = interpreter.interpret(
                "ask elsa to go where people are gathering",
                List.of(human(1L, "Elsa"))
        );

        assertThat(match.status()).isEqualTo(FallbackCommandMatchStatus.INVALID);
        assertThat(match.command()).isNull();
    }

    private Human human(Long id, String name) {
        Human human = new Human();
        human.setId(id);
        human.setName(name);
        City city = new City();
        city.setId(1L);
        human.setCity(city);
        return human;
    }
}
