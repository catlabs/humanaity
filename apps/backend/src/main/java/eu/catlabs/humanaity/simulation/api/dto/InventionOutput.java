package eu.catlabs.humanaity.simulation.api.dto;

import eu.catlabs.humanaity.history.domain.HistoryEra;
import eu.catlabs.humanaity.invention.domain.InventionCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventionOutput {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long cityId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long tickCreated;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private InventionCategory category;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String inventionKey;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String summary;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> sourceEventKeys;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer impactScore;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer yearCreated;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private HistoryEra eraCreated;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant createdAt;
}
