package eu.catlabs.humanaity.city.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import eu.catlabs.humanaity.auth.infrastructure.security.JwtService;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.domain.HumanTribeRole;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeHouseRepository;
import eu.catlabs.humanaity.simulation.infrastructure.persistence.TribeKnownPlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:city-create-api;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key"
})
@AutoConfigureMockMvc
class CityCreationApiContractTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private HumanRepository humanRepository;
    @Autowired
    private TribeHouseRepository tribeHouseRepository;
    @Autowired
    private TribeKnownPlaceRepository tribeKnownPlaceRepository;
    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        tribeKnownPlaceRepository.deleteAll();
        tribeHouseRepository.deleteAll();
        humanRepository.deleteAll();
        cityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createCityBootstrapsDeterministicHumansAcrossTwoTribes() throws Exception {
        User owner = persistUser("city-create-owner@example.com");

        MvcResult result = mockMvc.perform(post("/api/cities")
                        .header("Authorization", bearerFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cityInput("Bootstrap City")))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        Long cityId = created.get("id").asLong();
        City city = cityRepository.findById(cityId).orElseThrow();
        List<Human> humans = humanRepository.findByCityIdOrderByIdAsc(city.getId());
        List<eu.catlabs.humanaity.simulation.domain.TribeHouse> houses = tribeHouseRepository.findByCityIdOrderByTribeIdAsc(city.getId());

        assertThat(humans).hasSize(6);
        assertThat(humans.stream().map(Human::getTribeId).collect(java.util.stream.Collectors.toSet()))
                .containsExactlyInAnyOrder("tribe-a", "tribe-b");
        assertThat(humans.stream().filter(human -> human.getTribeRole() == HumanTribeRole.CHIEF).count()).isEqualTo(2L);
        assertThat(humans.stream().filter(human -> human.getTribeRole() == HumanTribeRole.SCOUT).count()).isEqualTo(2L);
        assertThat(humans.stream().filter(human -> human.getTribeRole() == HumanTribeRole.MEMBER).count()).isEqualTo(2L);
        assertThat(humans.stream().filter(human -> "tribe-a".equals(human.getTribeId()) && human.getTribeRole() == HumanTribeRole.CHIEF).count()).isEqualTo(1L);
        assertThat(humans.stream().filter(human -> "tribe-a".equals(human.getTribeId()) && human.getTribeRole() == HumanTribeRole.SCOUT).count()).isEqualTo(1L);
        assertThat(humans.stream().filter(human -> "tribe-a".equals(human.getTribeId()) && human.getTribeRole() == HumanTribeRole.MEMBER).count()).isEqualTo(1L);
        assertThat(humans.stream().filter(human -> "tribe-b".equals(human.getTribeId()) && human.getTribeRole() == HumanTribeRole.CHIEF).count()).isEqualTo(1L);
        assertThat(humans.stream().filter(human -> "tribe-b".equals(human.getTribeId()) && human.getTribeRole() == HumanTribeRole.SCOUT).count()).isEqualTo(1L);
        assertThat(humans.stream().filter(human -> "tribe-b".equals(human.getTribeId()) && human.getTribeRole() == HumanTribeRole.MEMBER).count()).isEqualTo(1L);
        assertThat(houses).hasSize(2);
        assertThat(houses.stream().map(eu.catlabs.humanaity.simulation.domain.TribeHouse::getTribeId).toList())
                .containsExactly("tribe-a", "tribe-b");
        double distance = Math.sqrt(Math.pow(houses.get(0).getX() - houses.get(1).getX(), 2) + Math.pow(houses.get(0).getY() - houses.get(1).getY(), 2));
        assertThat(distance).isGreaterThanOrEqualTo(0.45);
        assertThat(humans.stream().map(Human::getX)).allMatch(value -> value != null && value >= 0.05 && value <= 0.95);
        assertThat(humans.stream().map(Human::getY)).allMatch(value -> value != null && value >= 0.05 && value <= 0.95);
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("hashed-password");
        user.setRoles(Set.of("ROLE_USER"));
        return userRepository.save(user);
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
