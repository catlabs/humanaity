package eu.catlabs.humanaity.simulation.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationTimelineSummaryOutput {
    private Long latestEventTick;
    private Long latestInventionTick;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer recentEventCount;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer recentInventionCount;
}
