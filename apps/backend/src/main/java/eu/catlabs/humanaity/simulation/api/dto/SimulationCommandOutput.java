package eu.catlabs.humanaity.simulation.api.dto;

import eu.catlabs.humanaity.agent.api.dto.AgentUiEffectOutput;

import java.util.ArrayList;
import java.util.List;

public class SimulationCommandOutput {

    private boolean ok;
    private String commandType;
    private String message;
    private boolean mutated;
    private SimulationCommandReferencedEntitiesOutput referencedEntities = new SimulationCommandReferencedEntitiesOutput();
    private List<AgentUiEffectOutput> uiEffects = new ArrayList<>();

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isMutated() {
        return mutated;
    }

    public void setMutated(boolean mutated) {
        this.mutated = mutated;
    }

    public SimulationCommandReferencedEntitiesOutput getReferencedEntities() {
        return referencedEntities;
    }

    public void setReferencedEntities(SimulationCommandReferencedEntitiesOutput referencedEntities) {
        this.referencedEntities = referencedEntities;
    }

    public List<AgentUiEffectOutput> getUiEffects() {
        return uiEffects;
    }

    public void setUiEffects(List<AgentUiEffectOutput> uiEffects) {
        this.uiEffects = uiEffects;
    }
}
