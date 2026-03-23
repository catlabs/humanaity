package eu.catlabs.humanaity.simulation.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TribeHouseOutput {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Double x;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Double y;
}
