package eu.catlabs.humanaity.ai.infrastructure.port;

import eu.catlabs.humanaity.ai.domain.AiProvider;
import eu.catlabs.humanaity.ai.domain.AiPrompt;
import eu.catlabs.humanaity.ai.domain.AiResponse;

/**
 * Port interface for AI providers.
 * This abstraction allows the application to work with any AI provider
 * without depending on specific implementations.
 * 
 * To add a new AI provider, implement this interface and register it as a Spring bean.
 */
public interface AiProviderPort {
    
    /**
     * Generate a response based on the provided prompt.
     * 
     * @param prompt The AI prompt containing system message, user message, etc.
     * @return The AI response
     * @throws AiServiceException if the AI service call fails
     */
    AiResponse generate(AiPrompt prompt) throws AiServiceException;
    
    /**
     * Get the provider type this adapter implements.
     * 
     * @return The AI provider enum value
     */
    AiProvider getProviderType();
    
    /**
     * Check if this provider is currently available.
     * 
     * @return true if the provider is available, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Get the priority of this provider (lower number = higher priority).
     * Used when multiple providers are available to select the preferred one.
     * 
     * @return Priority value (lower = higher priority)
     */
    default int getPriority() {
        return getProviderType().getDefaultPriority();
    }
}
