package eu.catlabs.humanaity.simulation.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationSnapshotMetricsOutput {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer population;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer busyCount;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Double busyRatio;
    private SimulationSnapshotCentroidOutput centroid;
    private SimulationSnapshotBoundsOutput bounds;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer eventCount;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer inventionCount;
}
