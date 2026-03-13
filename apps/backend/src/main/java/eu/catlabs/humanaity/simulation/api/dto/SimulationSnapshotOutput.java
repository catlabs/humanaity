package eu.catlabs.humanaity.simulation.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationSnapshotOutput {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private SimulationSnapshotCityOutput city;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private SimulationSnapshotRunOutput run;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private SimulationTimelineSummaryOutput timelineSummary;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SimulationSnapshotHumanOutput> humans;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private SimulationSnapshotMetricsOutput metrics;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<EventOutput> recentEvents;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<InventionOutput> recentInventions;
}
