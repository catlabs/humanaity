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
        if (type == SimulationAssistantCommandType.CHIEF_PLAN) {
            return new SimulationAssistantCommandDescriptorOutput(
                    type.name(),
                    "si chef",
                    "Chief plan",
                    "Current persisted chief plan per tribe."
            );
        }
        if (type == SimulationAssistantCommandType.INVENTIONS) {
            return new SimulationAssistantCommandDescriptorOutput(
                    type.name(),
                    "inventions",
                    "Inventions",
                    "Latest unlocked inventions in deterministic order."
            );
        }
        if (type == SimulationAssistantCommandType.WORLD_STATUS) {
            return new SimulationAssistantCommandDescriptorOutput(
                    type.name(),
                    "world status",
                    "World status",
                    "High-level simulation run and population snapshot."
            );
        }
        if (type == SimulationAssistantCommandType.RECENT_EVENTS) {
            return new SimulationAssistantCommandDescriptorOutput(
                    type.name(),
                    "recent events",
                    "Recent events",
                    "Latest events from the simulation history window."
            );
        }
        if (type == SimulationAssistantCommandType.RELATIONSHIPS) {
            return new SimulationAssistantCommandDescriptorOutput(
                    type.name(),
                    "relationships",
                    "Relationships",
                    "Interaction pairs derived from recent dialogue and collision events."
            );
        }
        if (type == SimulationAssistantCommandType.AI_LOGS) {
            return new SimulationAssistantCommandDescriptorOutput(
                    type.name(),
                    "ai logs",
                    "AI logs",
                    "Recent persisted AI and LLM call entries."
            );
        }
        if (type == SimulationAssistantCommandType.AI_STATS) {
            return new SimulationAssistantCommandDescriptorOutput(
                    type.name(),
                    "ai stats",
                    "AI stats",
                    "App-wide summary of AI and LLM usage."
            );
        }
        throw new IllegalStateException("Unsupported is not a listable command");
    }
}
