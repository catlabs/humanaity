package eu.catlabs.humanaity.simulation.application.assistant;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationAssistantCommandInterpreterTest {

    private final SimulationAssistantCommandInterpreter interpreter = new SimulationAssistantCommandInterpreter();

    @ParameterizedTest
    @ValueSource(strings = {"si chef", "chef", "chief", "chief plan"})
    void recognizesChiefPlanAliases(String input) {
        SimulationAssistantCommandInterpretation interpretation = interpreter.interpret(input);

        assertThat(interpretation.commandType()).isEqualTo(SimulationAssistantCommandType.CHIEF_PLAN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ai logs", "AI logs", "llm logs"})
    void recognizesAiLogAliases(String input) {
        SimulationAssistantCommandInterpretation interpretation = interpreter.interpret(input);

        assertThat(interpretation.commandType()).isEqualTo(SimulationAssistantCommandType.AI_LOGS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ai stats", "AI stats", "llm stats", "llm usage", "ai usage"})
    void recognizesAiStatsAliases(String input) {
        SimulationAssistantCommandInterpretation interpretation = interpreter.interpret(input);

        assertThat(interpretation.commandType()).isEqualTo(SimulationAssistantCommandType.AI_STATS);
    }
}
