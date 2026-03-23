package eu.catlabs.humanaity.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:openapi-contract;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
@AutoConfigureMockMvc(addFilters = false)
class OpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void openApiReflectsDisabledSignupAndTypedSharedSimulationEndpoints() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String openApiJson = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(openApiJson);

        assertThat(root.at("/paths/~1auth~1login/post").isMissingNode()).isFalse();
        assertThat(root.at("/paths/~1auth~1signup").isMissingNode()).isTrue();
        assertThat(openApiJson).contains("\"/api/simulations/{cityId}/commands\"");
        assertThat(openApiJson).contains("\"$ref\":\"#/components/schemas/SimulationCommandOutput\"");
        assertThat(openApiJson).contains("\"$ref\":\"#/components/schemas/SimulationAssistantResponseOutput\"");
        assertThat(openApiJson).contains("\"$ref\":\"#/components/schemas/AgentChatResponseOutput\"");

        Path outputDirectory = Path.of("target", "openapi");
        Files.createDirectories(outputDirectory);
        Files.writeString(outputDirectory.resolve("openapi.json"), openApiJson);
    }
}
