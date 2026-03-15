package eu.catlabs.humanaity.agent.api.dto;

import java.util.List;

public class AgentUiEffectOutput {

    private String type;
    private Long humanId;
    private Long eventId;
    private Long inventionId;
    private Long fromTick;
    private String panel;
    private String placeId;
    private String eventType;
    private List<Long> eventIds;

    public AgentUiEffectOutput() {
    }

    public AgentUiEffectOutput(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getHumanId() {
        return humanId;
    }

    public void setHumanId(Long humanId) {
        this.humanId = humanId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getInventionId() {
        return inventionId;
    }

    public void setInventionId(Long inventionId) {
        this.inventionId = inventionId;
    }

    public Long getFromTick() {
        return fromTick;
    }

    public void setFromTick(Long fromTick) {
        this.fromTick = fromTick;
    }

    public String getPanel() {
        return panel;
    }

    public void setPanel(String panel) {
        this.panel = panel;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public List<Long> getEventIds() {
        return eventIds;
    }

    public void setEventIds(List<Long> eventIds) {
        this.eventIds = eventIds;
    }
}
