package eu.catlabs.humanaity.simulation.api.dto;

import java.util.List;

public record SimulationCommandBuilderActionOutput(
        String actionKey,
        String label,
        String executionKind,
        String actorKind,
        String targetKind,
        String commandText,
        String commandVerb,
        boolean requiresDifferentTarget,
        List<SimulationCommandBuilderOptionOutput> targetOptions
) {
}
