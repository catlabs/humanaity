package eu.catlabs.humanaity.ai.infrastructure.adapter.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.ai.domain.AiProvider;
import eu.catlabs.humanaity.ai.domain.AiPrompt;
import eu.catlabs.humanaity.ai.domain.AiResponse;
import eu.catlabs.humanaity.ai.infrastructure.port.AiProviderPort;
import eu.catlabs.humanaity.ai.infrastructure.port.AiServiceException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * OpenAI adapter implementing the AiProviderPort interface.
 * This adapter uses Spring AI's ChatClient to communicate with OpenAI.
 */
@Component
public class OpenAiAdapter implements AiProviderPort {
    
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    
    public OpenAiAdapter(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }
    
    @Override
    public AiResponse generate(AiPrompt prompt) throws AiServiceException {
        try {
            long startTime = System.currentTimeMillis();
            
            // Build the chat request
            var chatRequest = chatClient.prompt();
            
            // Add system message if provided
            if (prompt.getSystemMessage() != null && !prompt.getSystemMessage().isBlank()) {
                chatRequest = chatRequest.system(prompt.getSystemMessage());
            }
            
            // Add user message
            String response = chatRequest
                    .user(prompt.getUserMessage())
                    .call()
                    .content();
            
            // Extract and parse JSON if needed
            String cleanJson = extractJsonFromResponse(response);
            JsonNode jsonNode = null;
            try {
                jsonNode = objectMapper.readTree(cleanJson);
            } catch (JsonProcessingException e) {
                // If JSON parsing fails, we'll just use the raw content
            }
            
            Duration responseTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
            
            return AiResponse.builder()
                    .rawContent(response)
                    .jsonContent(jsonNode)
                    .provider(AiProvider.OPENAI)
                    .responseTime(responseTime)
                    .build();
                    
        } catch (Exception e) {
            throw new AiServiceException("OpenAI service error: " + e.getMessage(), e);
        }
    }
    
    @Override
    public AiProvider getProviderType() {
        return AiProvider.OPENAI;
    }
    
    @Override
    public boolean isAvailable() {
        // Check if ChatClient is available (basic availability check)
        return chatClient != null;
    }
    
    /**
     * Extracts JSON from AI response, handling cases where the response
     * may contain markdown formatting or extra text.
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.isBlank()) {
            return response;
        }
        
        // Try to extract array first
        int arrayStart = response.indexOf("[");
        int arrayEnd = response.lastIndexOf("]") + 1;
        
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return response.substring(arrayStart, arrayEnd);
        }
        
        // Fall back to object extraction
        int objectStart = response.indexOf("{");
        int objectEnd = response.lastIndexOf("}") + 1;
        
        if (objectStart >= 0 && objectEnd > objectStart) {
            return response.substring(objectStart, objectEnd);
        }
        
        // Return original response if no JSON found
        return response;
    }
}
