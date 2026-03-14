package eu.catlabs.humanaity.agent.api;

import eu.catlabs.humanaity.agent.api.dto.AgentChatRequestInput;
import eu.catlabs.humanaity.agent.api.dto.AgentChatResponseOutput;
import eu.catlabs.humanaity.agent.application.AgentChatOrchestrationService;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

    public AgentChatController(
            AgentChatOrchestrationService agentChatOrchestrationService,
            UserRepository userRepository
    ) {
        this.agentChatOrchestrationService = agentChatOrchestrationService;
        this.userRepository = userRepository;
    }

    @PostMapping("/cities/{cityId}/chat")
    @Operation(summary = "Process one city-scoped agent chat request")
    public ResponseEntity<AgentChatResponseOutput> chat(
            @PathVariable Long cityId,
            @RequestBody AgentChatRequestInput input,
            Authentication authentication
    ) {
        try {
            User currentUser = resolveCurrentUser(authentication);
            AgentChatResponseOutput response = agentChatOrchestrationService.orchestrate(cityId, currentUser, input);
            return ResponseEntity.ok(response);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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

    private static class UnauthorizedException extends RuntimeException {
    }
}
