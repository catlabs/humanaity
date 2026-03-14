package eu.catlabs.humanaity.city.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import eu.catlabs.humanaity.auth.infrastructure.security.JwtService;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:city-authz-api;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
@AutoConfigureMockMvc
class CityAuthorizationApiContractTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        cityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void updateCityRejectsUnauthenticatedCaller() throws Exception {
        User owner = persistUser("owner-update-unauth@example.com");
        City city = persistCity("OwnerTown", owner);

        mockMvc.perform(put("/api/cities/{id}", city.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cityInput("Unauthorized Update")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateCityReturnsForbiddenForNonOwner() throws Exception {
        User owner = persistUser("owner-update-forbidden@example.com");
        User otherUser = persistUser("other-update-forbidden@example.com");
        City city = persistCity("OwnerTown", owner);

        mockMvc.perform(put("/api/cities/{id}", city.getId())
                        .header("Authorization", bearerFor(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cityInput("Hijacked City")))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCityAllowsOwner() throws Exception {
        User owner = persistUser("owner-update-ok@example.com");
        City city = persistCity("InitialName", owner);

        mockMvc.perform(put("/api/cities/{id}", city.getId())
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cityInput("RenamedCity")))
                .andExpect(status().isOk());

        City updated = cityRepository.findById(city.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("RenamedCity");
    }

    @Test
    void updateCityReturnsNotFoundWhenCityDoesNotExist() throws Exception {
        User owner = persistUser("owner-update-missing@example.com");

        mockMvc.perform(put("/api/cities/{id}", 999_999L)
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cityInput("MissingCity")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCityRejectsUnauthenticatedCaller() throws Exception {
        User owner = persistUser("owner-delete-unauth@example.com");
        City city = persistCity("DeleteTown", owner);

        mockMvc.perform(delete("/api/cities/{id}", city.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteCityReturnsForbiddenForNonOwner() throws Exception {
        User owner = persistUser("owner-delete-forbidden@example.com");
        User otherUser = persistUser("other-delete-forbidden@example.com");
        City city = persistCity("DeleteTown", owner);

        mockMvc.perform(delete("/api/cities/{id}", city.getId())
                        .header("Authorization", bearerFor(otherUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCityAllowsOwner() throws Exception {
        User owner = persistUser("owner-delete-ok@example.com");
        City city = persistCity("DeleteTown", owner);

        mockMvc.perform(delete("/api/cities/{id}", city.getId())
                        .header("Authorization", bearerFor(owner)))
                .andExpect(status().isNoContent());

        assertThat(cityRepository.findById(city.getId())).isEmpty();
    }

    @Test
    void deleteCityReturnsNotFoundWhenCityDoesNotExist() throws Exception {
        User owner = persistUser("owner-delete-missing@example.com");

        mockMvc.perform(delete("/api/cities/{id}", 999_999L)
                        .header("Authorization", bearerFor(owner)))
                .andExpect(status().isNotFound());
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("hashed-password");
        user.setRoles(Set.of("ROLE_USER"));
        return userRepository.save(user);
    }

    private City persistCity(String name, User owner) {
        City city = new City();
        city.setName(name);
        city.setOwner(owner);
        return cityRepository.save(city);
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtService.generateAccessToken(user.getEmail());
    }

    private String cityInput(String name) throws Exception {
        return objectMapper.writeValueAsString(new CityInputPayload(name));
    }

    private record CityInputPayload(String name) {
    }
}
