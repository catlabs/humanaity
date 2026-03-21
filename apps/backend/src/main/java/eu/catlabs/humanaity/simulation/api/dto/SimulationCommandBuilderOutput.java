package eu.catlabs.humanaity.simulation.api.dto;

import java.util.List;

public record SimulationCommandBuilderOutput(
        List<SimulationCommandBuilderOptionOutput> actorOptions,
        List<SimulationCommandBuilderActionOutput> actions
) {
}
