package eu.catlabs.humanaity.human.api.dto;

import eu.catlabs.humanaity.human.domain.Personality;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HumanInput {
    private boolean busy;
    
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;
    
    @DecimalMin(value = "0.0", message = "X must be >= 0")
    @DecimalMax(value = "1.0", message = "X must be <= 1")
    private Double x;
    
    @DecimalMin(value = "0.0", message = "Y must be >= 0")
    @DecimalMax(value = "1.0", message = "Y must be <= 1")
    private Double y;

    // Personality traits (0.0 to 1.0)
    @DecimalMin(value = "0.0", message = "Creativity must be >= 0")
    @DecimalMax(value = "1.0", message = "Creativity must be <= 1")
    private Double creativity = 0.5;
    
    @DecimalMin(value = "0.0", message = "Intellect must be >= 0")
    @DecimalMax(value = "1.0", message = "Intellect must be <= 1")
    private Double intellect = 0.5;
    
    @DecimalMin(value = "0.0", message = "Sociability must be >= 0")
    @DecimalMax(value = "1.0", message = "Sociability must be <= 1")
    private Double sociability = 0.5;
    
    @DecimalMin(value = "0.0", message = "Practicality must be >= 0")
    @DecimalMax(value = "1.0", message = "Practicality must be <= 1")
    private Double practicality = 0.5;
    
    private Personality personality;

    @NotNull(message = "City ID is required")
    private Long cityId;
}
