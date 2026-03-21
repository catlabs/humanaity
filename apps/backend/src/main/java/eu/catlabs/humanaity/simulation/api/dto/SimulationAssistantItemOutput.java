package eu.catlabs.humanaity.simulation.api.dto;

import java.util.ArrayList;
import java.util.List;

public class SimulationAssistantItemOutput {

    private String title;
    private String subtitle;
    private String body;
    private List<String> chips = new ArrayList<>();

    public SimulationAssistantItemOutput() {
    }

    public SimulationAssistantItemOutput(String title, String subtitle, String body, List<String> chips) {
        this.title = title;
        this.subtitle = subtitle;
        this.body = body;
        this.chips = chips == null ? new ArrayList<>() : chips;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<String> getChips() {
        return chips;
    }

    public void setChips(List<String> chips) {
        this.chips = chips == null ? new ArrayList<>() : chips;
    }
}
