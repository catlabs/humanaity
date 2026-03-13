package eu.catlabs.humanaity.simulation.api.dto;

import eu.catlabs.humanaity.history.domain.HistoryEra;
import eu.catlabs.humanaity.simulation.domain.SimulationRunStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CityOverviewOutput {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long cityId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String cityName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasRun;
    private SimulationRunStatus runStatus;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean running;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long tick;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer year;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private HistoryEra era;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer population;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer inventionCount;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer eventCount;
    private Instant updatedAt;
}
