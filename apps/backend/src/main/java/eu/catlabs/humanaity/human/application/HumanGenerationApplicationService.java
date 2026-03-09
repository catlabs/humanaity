package eu.catlabs.humanaity.human.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import eu.catlabs.humanaity.human.api.dto.HumanInput;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.ai.application.AiGenerationService;
import eu.catlabs.humanaity.ai.application.prompt.HumanGenerationPrompt;
import eu.catlabs.humanaity.ai.domain.AiPrompt;
import eu.catlabs.humanaity.ai.domain.AiResponse;
import eu.catlabs.humanaity.ai.infrastructure.port.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class HumanGenerationApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(HumanGenerationApplicationService.class);

    private final AiGenerationService aiGenerationService;
    private final HumanGenerationPrompt promptBuilder;
    private final HumanApplicationService humanApplicationService;
    private final Faker faker = new Faker(new Locale("en"));
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    public HumanGenerationApplicationService(AiGenerationService aiGenerationService,
                                            HumanGenerationPrompt promptBuilder,
                                            HumanApplicationService humanApplicationService) {
        this.aiGenerationService = aiGenerationService;
        this.promptBuilder = promptBuilder;
        this.humanApplicationService = humanApplicationService;
    }

    @Transactional
    public void generateHumansForCity(City city) {
        if (city == null || city.getId() == null) {
            throw new IllegalArgumentException("City must be saved before generating humans");
        }
        
        try {
            JsonNode humans;
            try {
                humans = generateHumansWithAiAsync();
                logger.info("Successfully generated humans using AI service for city {}", city.getId());
            } catch (Exception e) {
                logger.warn("OpenAI service unavailable, creating test data manually: {}", e.getMessage());
                humans = createHumansManually();
            }

            if (humans == null || !humans.isArray()) {
                logger.error("Invalid humans data structure for city {}", city.getId());
                throw new RuntimeException("Invalid humans data structure");
            }

            int createdCount = 0;
            for (JsonNode node : humans) {
                try {
                    HumanInput humanInput = new HumanInput();
                    humanInput.setName(node.get("name").asText());
                    humanInput.setCityId(city.getId());
                    humanInput.setBusy(false);
                    humanInput.setCreativity(node.get("creativity").asDouble());
                    humanInput.setIntellect(node.get("intellect").asDouble());
                    humanInput.setSociability(node.get("sociability").asDouble());
                    humanInput.setPracticality(node.get("practicality").asDouble());
                    humanInput.setPersonality(this.humanApplicationService.derivePersonality(
                            humanInput.getCreativity(),
                            humanInput.getIntellect(),
                            humanInput.getSociability(),
                            humanInput.getPracticality()
                    ));
                    humanInput.setX(faker.number().randomDouble(3, 0, 1));
                    humanInput.setY(faker.number().randomDouble(3, 0, 1));
                    this.humanApplicationService.createHuman(humanInput);
                    createdCount++;
                } catch (Exception e) {
                    logger.error("Error creating human for city {}: {}", city.getId(), e.getMessage(), e);
                }
            }
            
            if (createdCount == 0) {
                throw new RuntimeException("Failed to create any humans for city " + city.getId());
            }
            
            logger.info("Successfully created {} humans for city {}", createdCount, city.getId());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error generating humans for city {}: {}", city.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate humans for city: " + e.getMessage(), e);
        }
    }

    private JsonNode generateHumansWithAiAsync() throws Exception {
        CompletableFuture<JsonNode> future = CompletableFuture.supplyAsync(() -> {
            try {
                AiPrompt prompt = promptBuilder.createHumanGenerationPrompt();
                AiResponse response = aiGenerationService.generate(prompt);
                JsonNode jsonContent = response.getJsonContent();
                if (jsonContent == null && response.getRawContent() != null) {
                    jsonContent = objectMapper.readTree(response.getRawContent());
                }
                return jsonContent;
            } catch (AiServiceException e) {
                throw new RuntimeException("AI service error: " + e.getMessage(), e);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("JSON parsing error: " + e.getMessage(), e);
            }
        });
        
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("AI service call timed out", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Error calling AI service", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("AI service call interrupted", e);
        }
    }

    private JsonNode createHumansManually() throws JsonProcessingException {
        com.fasterxml.jackson.databind.node.ArrayNode arrayNode = objectMapper.createArrayNode();
        
        for (int i = 0; i < 5; i++) {
            double creativity = random.nextDouble();
            double intellect = random.nextDouble();
            double sociability = random.nextDouble();
            double practicality = 2.0 - creativity - intellect - sociability;

            double sum = creativity + intellect + sociability + practicality;
            creativity = (creativity / sum) * 2.0;
            intellect = (intellect / sum) * 2.0;
            sociability = (sociability / sum) * 2.0;
            practicality = (practicality / sum) * 2.0;

            com.fasterxml.jackson.databind.node.ObjectNode humanNode = objectMapper.createObjectNode();
            humanNode.put("name", faker.name().fullName());
            humanNode.put("creativity", Math.round(creativity * 100.0) / 100.0);
            humanNode.put("intellect", Math.round(intellect * 100.0) / 100.0);
            humanNode.put("sociability", Math.round(sociability * 100.0) / 100.0);
            humanNode.put("practicality", Math.round(practicality * 100.0) / 100.0);
            humanNode.put("personality", "BALANCED");
            
            arrayNode.add(humanNode);
        }
        
        return arrayNode;
    }
}
