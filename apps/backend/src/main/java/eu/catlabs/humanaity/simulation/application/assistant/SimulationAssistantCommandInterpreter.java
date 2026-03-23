package eu.catlabs.humanaity.simulation.application.assistant;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class SimulationAssistantCommandInterpreter {

    public SimulationAssistantCommandInterpretation interpret(String rawCommand) {
        String safeRawCommand = rawCommand == null ? "" : rawCommand.trim();
        String normalized = normalize(safeRawCommand);

        SimulationAssistantCommandType commandType = switch (normalized) {
            case "sichef", "chef", "chief", "chiefplan" ->
                    SimulationAssistantCommandType.CHIEF_PLAN;
            case "inventions", "inventory", "inventoryofinventions", "inventioninventory" ->
                    SimulationAssistantCommandType.INVENTIONS;
            case "worldstatus", "world", "status", "simulationstatus", "worldsummary" ->
                    SimulationAssistantCommandType.WORLD_STATUS;
            case "recentevents", "events", "latestevents" ->
                    SimulationAssistantCommandType.RECENT_EVENTS;
            case "relationships", "relations", "interactions" ->
                    SimulationAssistantCommandType.RELATIONSHIPS;
            case "ailogs", "aialogs", "llmlogs" ->
                    SimulationAssistantCommandType.AI_LOGS;
            case "aistats", "llmstats", "llmusage", "aiusage" ->
                    SimulationAssistantCommandType.AI_STATS;
            default -> SimulationAssistantCommandType.UNSUPPORTED;
        };

        return new SimulationAssistantCommandInterpretation(commandType, normalized, safeRawCommand);
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replace(" ", "");
    }
}
