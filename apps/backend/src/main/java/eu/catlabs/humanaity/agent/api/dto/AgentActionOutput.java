package eu.catlabs.humanaity.agent.api.dto;

public class AgentActionOutput {

    private String type;
    private String status;
    private String summary;

    public AgentActionOutput() {
    }

    public AgentActionOutput(String type, String status, String summary) {
        this.type = type;
        this.status = status;
        this.summary = summary;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
