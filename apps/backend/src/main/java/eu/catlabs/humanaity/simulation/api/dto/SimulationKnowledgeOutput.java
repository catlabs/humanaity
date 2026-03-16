package eu.catlabs.humanaity.simulation.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationKnowledgeOutput {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> unlockedDiscoveries;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> unlockedInventions;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> unlockedApplications;
}
