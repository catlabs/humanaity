package eu.catlabs.humanaity.simulation.application.assistant;

public record SimulationAssistantCommandInterpretation(
        SimulationAssistantCommandType commandType,
        String normalizedCommand,
        String rawCommand
) {
}
