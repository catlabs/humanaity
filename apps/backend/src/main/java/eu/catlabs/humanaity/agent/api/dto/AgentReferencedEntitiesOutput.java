package eu.catlabs.humanaity.agent.api.dto;

import java.util.ArrayList;
import java.util.List;

public class AgentReferencedEntitiesOutput {

    private Long cityId;
    private List<Long> humanIds = new ArrayList<>();
    private List<Long> eventIds = new ArrayList<>();
    private List<Long> inventionIds = new ArrayList<>();

    public Long getCityId() {
        return cityId;
    }

    public void setCityId(Long cityId) {
        this.cityId = cityId;
    }

    public List<Long> getHumanIds() {
        return humanIds;
    }

    public void setHumanIds(List<Long> humanIds) {
        this.humanIds = humanIds;
    }

    public List<Long> getEventIds() {
        return eventIds;
    }

    public void setEventIds(List<Long> eventIds) {
        this.eventIds = eventIds;
    }

    public List<Long> getInventionIds() {
        return inventionIds;
    }

    public void setInventionIds(List<Long> inventionIds) {
        this.inventionIds = inventionIds;
    }
}
