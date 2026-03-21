package eu.catlabs.humanaity.simulation.application.assistant;

import eu.catlabs.humanaity.simulation.api.dto.SimulationAssistantCommandDescriptorOutput;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Canonical list of assistant commands the backend interprets deterministically.
 * Keep in sync with {@link SimulationAssistantCommandInterpreter}.
 */
@Component
public class SimulationAssistantCommandsCatalog {

    public List<SimulationAssistantCommandDescriptorOutput> listSupportedCommands() {
        return Arrays.stream(SimulationAssistantCommandType.values())
                .filter(type -> type != SimulationAssistantCommandType.UNSUPPORTED)
                .map(this::toDescriptor)
                .toList();
    }

    private SimulationAssistantCommandDescriptorOutput toDescriptor(SimulationAssistantCommandType type) {
        return switch (type) {
            case INVENTIONS -> new SimulationAssistantCommandDescriptorOutput(
                    type.name(),
                    "inventions",
                    "Inventions",
                    "Latest unlocked inventions in deterministic order."
            );
            case WORLD_STATUS -> new SimulationAssistantCommandDescriptorOutput(
                    type.name(),
                    "world status",
                    "World status",
                    "High-level simulation run and population snapshot."
            );
            case RECENT_EVENTS -> new SimulationAssistantCommandDescriptorOutput(
                    type.name(),
                    "recent events",
                    "Recent events",
                    "Latest events from the simulation history window."
            );
            case RELATIONSHIPS -> new SimulationAssistantCommandDescriptorOutput(
                    type.name(),
                    "relationships",
                    "Relationships",
                    "Interaction pairs derived from recent dialogue and collision events."
            );
            case UNSUPPORTED -> throw new IllegalStateException("Unsupported is not a listable command");
        };
    }
}
