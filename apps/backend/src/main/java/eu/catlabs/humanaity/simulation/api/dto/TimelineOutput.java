package eu.catlabs.humanaity.simulation.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimelineOutput {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long cityId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fromTick;
    private Long toTick;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer eventCount;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer inventionCount;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<EventOutput> events;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<InventionOutput> inventions;
}
