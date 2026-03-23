package eu.catlabs.humanaity.ai.api;

import eu.catlabs.humanaity.ai.api.dto.AiCallLogContextSummaryOutput;
import eu.catlabs.humanaity.ai.api.dto.AiCallLogOutput;
import eu.catlabs.humanaity.ai.api.dto.AiCallLogSummaryOutput;
import eu.catlabs.humanaity.ai.application.AiCallLogFilter;
import eu.catlabs.humanaity.ai.application.AiCallLogService;
import eu.catlabs.humanaity.ai.domain.AiCallContextType;
import eu.catlabs.humanaity.ai.domain.AiCallLog;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI", description = "AI observability API")
@SecurityRequirement(name = "bearer-jwt")
public class AiController {

    private final AiCallLogService aiCallLogService;
    private final UserRepository userRepository;

    public AiController(AiCallLogService aiCallLogService, UserRepository userRepository) {
        this.aiCallLogService = aiCallLogService;
        this.userRepository = userRepository;
    }

    @GetMapping("/logs")
    @Operation(summary = "List persisted AI call logs")
    public ResponseEntity<List<AiCallLogOutput>> listLogs(
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) AiCallContextType contextType,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) Boolean fallbackUsed,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) Integer limit,
            Authentication authentication
    ) {
        try {
            resolveCurrentUser(authentication);
            return ResponseEntity.ok(aiCallLogService.list(new AiCallLogFilter(
                    cityId,
                    contextType,
                    success,
                    fallbackUsed,
                    provider,
                    model,
                    limit
            )).stream().map(this::toOutput).toList());
        } catch (UnauthorizedException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/logs/summary")
    @Operation(summary = "Aggregate persisted AI call logs")
    public ResponseEntity<AiCallLogSummaryOutput> summarizeLogs(
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) AiCallContextType contextType,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) Boolean fallbackUsed,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model,
            Authentication authentication
    ) {
        try {
            resolveCurrentUser(authentication);
            AiCallLogService.AiCallLogSummary summary = aiCallLogService.summarize(new AiCallLogFilter(
                    cityId,
                    contextType,
                    success,
                    fallbackUsed,
                    provider,
                    model,
                    null
            ));
            return ResponseEntity.ok(new AiCallLogSummaryOutput(
                    summary.totalCount(),
                    summary.successCount(),
                    summary.failureCount(),
                    summary.fallbackCount(),
                    summary.byContextType().stream()
                            .map(contextSummary -> new AiCallLogContextSummaryOutput(
                                    contextSummary.contextType().name(),
                                    contextSummary.totalCount(),
                                    contextSummary.successCount(),
                                    contextSummary.failureCount(),
                                    contextSummary.fallbackCount()
                            ))
                            .toList()
            ));
        } catch (UnauthorizedException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private AiCallLogOutput toOutput(AiCallLog log) {
        return new AiCallLogOutput(
                log.getId(),
                log.getRequestedAt(),
                log.getCity() == null ? null : log.getCity().getId(),
                log.getContextType() == null ? null : log.getContextType().name(),
                log.getContextEntityType(),
                log.getContextEntityId(),
                log.getProvider(),
                log.getModel(),
                log.isSuccess(),
                log.isFallbackUsed(),
                log.getDurationMs(),
                log.getPromptSummary(),
                log.getPromptHash(),
                log.getResponseHash(),
                log.getErrorCode(),
                log.getErrorMessage()
        );
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
