package eu.catlabs.humanaity.ai.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Domain model representing an AI prompt request.
 * This encapsulates the prompt data in a provider-agnostic way.
 */
public class AiPrompt {
    private String systemMessage;
    private String userMessage;
    private Map<String, Object> parameters;
    private ResponseFormat responseFormat;
    
    private AiPrompt() {
        this.parameters = new HashMap<>();
    }
    
    public static AiPromptBuilder builder() {
        return new AiPromptBuilder();
    }
    
    public String getSystemMessage() {
        return systemMessage;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public ResponseFormat getResponseFormat() {
        return responseFormat;
    }
    
    /**
     * Builder for creating AiPrompt instances
     */
    public static class AiPromptBuilder {
        private final AiPrompt prompt = new AiPrompt();
        
        public AiPromptBuilder systemMessage(String systemMessage) {
            prompt.systemMessage = systemMessage;
            return this;
        }
        
        public AiPromptBuilder userMessage(String userMessage) {
            prompt.userMessage = userMessage;
            return this;
        }
        
        public AiPromptBuilder parameter(String key, Object value) {
            prompt.parameters.put(key, value);
            return this;
        }
        
        public AiPromptBuilder responseFormat(ResponseFormat format) {
            prompt.responseFormat = format;
            return this;
        }
        
        public AiPrompt build() {
            return prompt;
        }
    }
    
    /**
     * Expected response format from AI
     */
    public enum ResponseFormat {
        JSON_OBJECT,
        JSON_ARRAY,
        TEXT
    }
}
