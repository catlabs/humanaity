package eu.catlabs.humanaity.simulation.api.dto;

/**
 * One supported deterministic assistant command, aligned with {@link eu.catlabs.humanaity.simulation.application.assistant.SimulationAssistantCommandInterpreter}.
 */
public record SimulationAssistantCommandDescriptorOutput(
        String commandType,
        String canonicalText,
        String label,
        String description
) {}
