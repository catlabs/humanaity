package eu.catlabs.humanaity.ai.application;

import eu.catlabs.humanaity.ai.domain.AiPrompt;
import eu.catlabs.humanaity.ai.domain.AiProvider;
import eu.catlabs.humanaity.ai.domain.AiResponse;
import eu.catlabs.humanaity.ai.infrastructure.port.AiProviderPort;
import eu.catlabs.humanaity.ai.infrastructure.port.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service for AI generation.
 * Orchestrates AI calls and manages provider selection.
 * 
 * Currently supports a single provider (OpenAI), but structured to easily
 * support multiple providers with fallback logic in the future.
 */
@Service
public class AiGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AiGenerationService.class);
    
    private final List<AiProviderPort> providers;
    private final AiCallLogService aiCallLogService;
    
    public AiGenerationService(List<AiProviderPort> providers, AiCallLogService aiCallLogService) {
        this.providers = providers;
        this.aiCallLogService = aiCallLogService;
        logger.info("Initialized AiGenerationService with {} provider(s)", providers.size());
    }
    
    /**
     * Generate content using the default/available provider.
     * 
     * @param prompt The AI prompt
     * @return The AI response
     * @throws AiServiceException if no provider is available or generation fails
     */
    public AiResponse generate(AiPrompt prompt) throws AiServiceException {
        return generate(prompt, AiGenerationContext.unspecified());
    }

    public AiResponse generate(AiPrompt prompt, AiGenerationContext context) throws AiServiceException {
        return generateInternal(prompt, context, null);
    }
    
    /**
     * Generate content using a specific provider.
     * 
     * @param providerType The provider to use
     * @param prompt The AI prompt
     * @return The AI response
     * @throws AiServiceException if the provider is not available or generation fails
     */
    public AiResponse generateWithProvider(AiProvider providerType, AiPrompt prompt) throws AiServiceException {
        return generateWithProvider(providerType, prompt, AiGenerationContext.unspecified());
    }

    public AiResponse generateWithProvider(AiProvider providerType, AiPrompt prompt, AiGenerationContext context) throws AiServiceException {
        return generateInternal(prompt, context, providerType);
    }

    public void markFallbackUsed(Long logId) {
        aiCallLogService.markFallbackUsed(logId);
    }
    
    /**
     * Select the best available provider.
     * Currently returns the first available provider.
     * In the future, this can implement priority-based selection.
     */
    private AiProviderPort selectProvider() {
        // Find first available provider
        for (AiProviderPort provider : providers) {
            if (provider.isAvailable()) {
                logger.debug("Selected AI provider: {}", provider.getProviderType());
                return provider;
            }
        }
        
        throw new AiServiceException("No available AI provider found");
    }
    
    /**
     * Find a provider by type.
     */
    private AiProviderPort findProvider(AiProvider providerType) {
        return providers.stream()
                .filter(p -> p.getProviderType() == providerType)
                .filter(AiProviderPort::isAvailable)
                .findFirst()
                .orElse(null);
    }

    private AiResponse generateInternal(AiPrompt prompt, AiGenerationContext context, AiProvider requestedProviderType) {
        long startedAt = System.currentTimeMillis();
        AiProviderPort provider = null;
        try {
            if (requestedProviderType == null) {
                provider = selectProvider();
            } else {
                provider = findProvider(requestedProviderType);
                if (provider == null) {
                    throw new AiServiceException("Provider " + requestedProviderType + " is not available");
                }
            }

            AiResponse response = provider.generate(prompt);
            long durationMs = resolveDurationMs(startedAt, response);
            var log = aiCallLogService.recordSuccess(
                    context == null ? AiGenerationContext.unspecified() : context,
                    prompt,
                    provider.getProviderType().name(),
                    response.getModel(),
                    response,
                    durationMs
            );
            response.setLogId(log.getId());
            return response;
        } catch (Exception ex) {
            AiServiceException wrapped = ex instanceof AiServiceException aiServiceException
                    ? aiServiceException
                    : new AiServiceException("AI generation failed: " + ex.getMessage(), ex);
            long durationMs = Math.max(0L, System.currentTimeMillis() - startedAt);
            aiCallLogService.recordFailure(
                    context == null ? AiGenerationContext.unspecified() : context,
                    prompt,
                    provider == null
                            ? (requestedProviderType == null ? null : requestedProviderType.name())
                            : provider.getProviderType().name(),
                    null,
                    durationMs,
                    wrapped
            );
            throw wrapped;
        }
    }

    private long resolveDurationMs(long startedAt, AiResponse response) {
        if (response != null && response.getResponseTime() != null) {
            return Math.max(response.getResponseTime().toMillis(), 0L);
        }
        return Math.max(0L, System.currentTimeMillis() - startedAt);
    }
}
