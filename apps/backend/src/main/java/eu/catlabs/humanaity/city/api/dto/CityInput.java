package eu.catlabs.humanaity.city.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CityInput {
    @NotBlank(message = "City name is required")
    @Size(min = 1, max = 100, message = "City name must be between 1 and 100 characters")
    private String name;
}
