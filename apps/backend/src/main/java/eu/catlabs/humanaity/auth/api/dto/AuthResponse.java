package eu.catlabs.humanaity.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String accessToken;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
