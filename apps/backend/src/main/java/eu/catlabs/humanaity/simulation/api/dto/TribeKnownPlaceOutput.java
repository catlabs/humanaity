package eu.catlabs.humanaity.simulation.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TribeKnownPlaceOutput {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String placeId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long discoveredByHumanId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long discoveredTick;
    private Long reportedTick;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean reported;
}
