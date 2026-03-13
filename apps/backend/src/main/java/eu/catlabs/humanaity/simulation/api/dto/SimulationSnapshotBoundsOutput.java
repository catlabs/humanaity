package eu.catlabs.humanaity.simulation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationSnapshotBoundsOutput {
    private Double minX;
    private Double maxX;
    private Double minY;
    private Double maxY;
}
