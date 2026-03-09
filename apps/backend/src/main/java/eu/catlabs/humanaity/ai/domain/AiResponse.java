package eu.catlabs.humanaity.ai.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * Domain model representing an AI response.
 * Contains the raw response and parsed JSON if applicable.
 */
public class AiResponse {
    private String rawContent;
    private JsonNode jsonContent;
    private AiProvider provider;
    private Duration responseTime;
    private Long tokensUsed;
    
    private AiResponse() {
    }
    
    public static AiResponseBuilder builder() {
        return new AiResponseBuilder();
    }
    
    public String getRawContent() {
        return rawContent;
    }
    
    /**
     * Get parsed JSON content. Returns null if content is not valid JSON.
     */
    public JsonNode getJsonContent(ObjectMapper objectMapper) {
        if (jsonContent == null && rawContent != null) {
            try {
                jsonContent = objectMapper.readTree(rawContent);
            } catch (JsonProcessingException e) {
                // Not valid JSON, return null
                return null;
            }
        }
        return jsonContent;
    }
    
    /**
     * Get JSON content that was already parsed during construction
     */
    public JsonNode getJsonContent() {
        return jsonContent;
    }
    
    public AiProvider getProvider() {
        return provider;
    }
    
    public Duration getResponseTime() {
        return responseTime;
    }
    
    public Long getTokensUsed() {
        return tokensUsed;
    }
    
    /**
     * Builder for creating AiResponse instances
     */
    public static class AiResponseBuilder {
        private final AiResponse response = new AiResponse();
        
        public AiResponseBuilder rawContent(String content) {
            response.rawContent = content;
            return this;
        }
        
        public AiResponseBuilder jsonContent(JsonNode jsonNode) {
            response.jsonContent = jsonNode;
            return this;
        }
        
        public AiResponseBuilder provider(AiProvider provider) {
            response.provider = provider;
            return this;
        }
        
        public AiResponseBuilder responseTime(Duration duration) {
            response.responseTime = duration;
            return this;
        }
        
        public AiResponseBuilder tokensUsed(Long tokens) {
            response.tokensUsed = tokens;
            return this;
        }
        
        public AiResponse build() {
            return response;
        }
    }
}
