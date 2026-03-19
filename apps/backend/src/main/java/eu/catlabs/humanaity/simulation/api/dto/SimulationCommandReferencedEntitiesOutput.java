package eu.catlabs.humanaity.simulation.api.dto;

public class SimulationCommandReferencedEntitiesOutput {

    private Long humanId;
    private String placeId;

    public Long getHumanId() {
        return humanId;
    }

    public void setHumanId(Long humanId) {
        this.humanId = humanId;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }
}
