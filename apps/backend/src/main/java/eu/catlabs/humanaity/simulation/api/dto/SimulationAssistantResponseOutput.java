package eu.catlabs.humanaity.simulation.api.dto;

import java.util.ArrayList;
import java.util.List;

public class SimulationAssistantResponseOutput {

    private boolean ok;
    private String commandType;
    private String text;
    private List<SimulationAssistantBlockOutput> blocks = new ArrayList<>();

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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<SimulationAssistantBlockOutput> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<SimulationAssistantBlockOutput> blocks) {
        this.blocks = blocks == null ? new ArrayList<>() : blocks;
    }
}
