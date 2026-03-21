package eu.catlabs.humanaity.simulation.api.dto;

import java.util.ArrayList;
import java.util.List;

public class SimulationAssistantBlockOutput {

    private String type;
    private String title;
    private String subtitle;
    private List<SimulationAssistantMetricOutput> metrics = new ArrayList<>();
    private List<SimulationAssistantItemOutput> items = new ArrayList<>();
    private String emptyState;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public List<SimulationAssistantMetricOutput> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<SimulationAssistantMetricOutput> metrics) {
        this.metrics = metrics == null ? new ArrayList<>() : metrics;
    }

    public List<SimulationAssistantItemOutput> getItems() {
        return items;
    }

    public void setItems(List<SimulationAssistantItemOutput> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public String getEmptyState() {
        return emptyState;
    }

    public void setEmptyState(String emptyState) {
        this.emptyState = emptyState;
    }
}
