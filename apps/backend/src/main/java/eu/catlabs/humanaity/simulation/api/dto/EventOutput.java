package eu.catlabs.humanaity.simulation.api.dto;

import eu.catlabs.humanaity.ai.domain.AiEnrichmentStatus;
import eu.catlabs.humanaity.event.domain.EventCategory;
import eu.catlabs.humanaity.event.domain.EventType;
import eu.catlabs.humanaity.history.domain.HistoryEra;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventOutput {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long cityId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long tick;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sequenceInTick;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private EventCategory eventCategory;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private EventType eventType;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> actorIds;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> payload;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer importance;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer year;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private HistoryEra era;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String eventKey;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String title;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String summary;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant createdAt;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private AiEnrichmentStatus enrichmentStatus;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean enrichmentFallback;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String enrichedSnippet;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String enrichmentProvider;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String enrichmentModel;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Instant enrichmentUpdatedAt;
}
