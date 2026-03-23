package eu.catlabs.humanaity.agent.api;

import eu.catlabs.humanaity.agent.api.dto.AgentChatRequestInput;
import eu.catlabs.humanaity.agent.api.dto.AgentChatResponseOutput;
import eu.catlabs.humanaity.agent.application.AgentChatOrchestrationService;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import eu.catlabs.humanaity.infrastructure.web.AbuseProtectionService;
import eu.catlabs.humanaity.infrastructure.web.ApiErrorResponse;
import eu.catlabs.humanaity.infrastructure.web.RateLimitExceededException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@Tag(name = "Agent Chat", description = "City-scoped agent orchestration API")
@SecurityRequirement(name = "bearer-jwt")
public class AgentChatController {

    private final AgentChatOrchestrationService agentChatOrchestrationService;
    private final UserRepository userRepository;
    private final AbuseProtectionService abuseProtectionService;

    public AgentChatController(
            AgentChatOrchestrationService agentChatOrchestrationService,
            UserRepository userRepository,
            AbuseProtectionService abuseProtectionService
    ) {
        this.agentChatOrchestrationService = agentChatOrchestrationService;
        this.userRepository = userRepository;
        this.abuseProtectionService = abuseProtectionService;
    }

    @PostMapping("/cities/{cityId}/chat")
    @Operation(summary = "Process one city-scoped agent chat request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agent chat response",
                    content = @Content(schema = @Schema(implementation = AgentChatResponseOutput.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<?> chat(
            @PathVariable Long cityId,
            @RequestBody AgentChatRequestInput input,
            Authentication authentication
    ) {
        try {
            abuseProtectionService.checkAgentChat(resolveCurrentSubject(authentication));
            User currentUser = resolveCurrentUser(authentication);
            AgentChatResponseOutput response = agentChatOrchestrationService.orchestrate(cityId, currentUser, input);
            return ResponseEntity.ok(response);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RateLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiErrorResponse(e.getMessage()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException();
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(UnauthorizedException::new);
    }

    private String resolveCurrentSubject(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException();
        }
        return authentication.getName();
    }

    private static class UnauthorizedException extends RuntimeException {
    }
}
