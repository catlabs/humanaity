package eu.catlabs.humanaity.ai.infrastructure.port;

/**
 * Exception thrown when an AI service operation fails.
 */
public class AiServiceException extends RuntimeException {
    
    public AiServiceException(String message) {
        super(message);
    }
    
    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
