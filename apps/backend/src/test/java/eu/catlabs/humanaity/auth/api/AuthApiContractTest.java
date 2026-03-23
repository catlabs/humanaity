package eu.catlabs.humanaity.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.RefreshTokenRepository;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-api;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
@AutoConfigureMockMvc
class AuthApiContractTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void loginStillWorksForExistingUser() throws Exception {
        User user = new User();
        user.setEmail("login-user@example.com");
        user.setPassword(passwordEncoder.encode("Test1234!"));
        userRepository.save(user);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthPayload("login-user@example.com", "Test1234!"))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payload.path("accessToken").asText()).isNotBlank();
        assertThat(payload.path("refreshToken").asText()).isNotBlank();
        assertThat(refreshTokenRepository.findAll()).hasSize(1);
    }

    @Test
    void signupEndpointIsUnavailable() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupPayload(
                                "disabled-signup@example.com",
                                "Test1234!",
                                "Test1234!"
                        ))))
                .andExpect(status().isGone());
    }

    private record AuthPayload(String email, String password) {
    }

    private record SignupPayload(String email, String password, String confirmPassword) {
    }
}
