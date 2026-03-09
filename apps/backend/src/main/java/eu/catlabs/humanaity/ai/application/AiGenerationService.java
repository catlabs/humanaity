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
    
    public AiGenerationService(List<AiProviderPort> providers) {
        this.providers = providers;
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
        AiProviderPort provider = selectProvider();
        return provider.generate(prompt);
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
        AiProviderPort provider = findProvider(providerType);
        if (provider == null) {
            throw new AiServiceException("Provider " + providerType + " is not available");
        }
        return provider.generate(prompt);
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
}
