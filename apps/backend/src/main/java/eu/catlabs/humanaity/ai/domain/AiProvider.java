package eu.catlabs.humanaity.ai.domain;

/**
 * Enumeration of supported AI providers.
 * Currently only OpenAI is implemented, but this enum is ready for future providers.
 */
public enum AiProvider {
    OPENAI("OpenAI", 1);
    // Future providers can be added here:
    // MISTRAL("Mistral AI", 2),
    // ANTHROPIC("Anthropic", 3);
    
    private final String displayName;
    private final int defaultPriority;
    
    AiProvider(String displayName, int defaultPriority) {
        this.displayName = displayName;
        this.defaultPriority = defaultPriority;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getDefaultPriority() {
        return defaultPriority;
    }
}
