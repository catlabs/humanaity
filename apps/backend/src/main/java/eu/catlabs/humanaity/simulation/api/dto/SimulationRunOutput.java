package eu.catlabs.humanaity.simulation.api.dto;

import eu.catlabs.humanaity.simulation.domain.SimulationRunStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationRunOutput {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long cityId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long seed;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long tick;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private SimulationRunStatus status;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean running;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant createdAt;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant updatedAt;
}
