package eu.catlabs.humanaity.simulation.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TribeSnapshotOutput {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String tribeId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private TribeHouseOutput house;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long scoutHumanId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<TribeKnownPlaceOutput> knownPlaces;
}
