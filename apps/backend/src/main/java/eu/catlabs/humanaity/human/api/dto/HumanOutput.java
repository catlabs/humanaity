package eu.catlabs.humanaity.human.api.dto;

import eu.catlabs.humanaity.human.domain.Personality;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HumanOutput {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
    @Schema(name = "busy", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean isBusy;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Double x;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Double y;

    private Double creativity;
    private Double intellect;
    private Double sociability;
    private Double practicality;
    private Personality personality;

    private Double scienceSkill;
    private Double cultureSkill;
    private Double socialSkill;

    private Double totalScienceContributed;
    private Double totalCultureContributed;
    private Double totalSocialContributed;
}
