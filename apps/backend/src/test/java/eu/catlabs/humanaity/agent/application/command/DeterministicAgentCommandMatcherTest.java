package eu.catlabs.humanaity.agent.application.command;

import eu.catlabs.humanaity.agent.api.dto.AgentChatRequestInput;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(DeterministicAgentCommandMatcher.class)
class DeterministicAgentCommandMatcherTest {

    @Autowired
    private DeterministicAgentCommandMatcher matcher;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private HumanRepository humanRepository;

    private City city;
    private Human elsa;
    private Human pierre;
    private Human lucas;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setEmail("matcher-owner@example.com");
        owner.setPassword("hash");
        owner = userRepository.save(owner);

        City savedCity = new City();
        savedCity.setName("Matcher City");
        savedCity.setOwner(owner);
        city = cityRepository.save(savedCity);

        elsa = saveHuman("Elsa");
        pierre = saveHuman("Pierre");
        lucas = saveHuman("Lucas");
    }

    @Test
    void matchesStepCommandDeterministically() {
        DeterministicCommandMatch match = matcher.match(city.getId(), request("step 10"));

        assertThat(match.status()).isEqualTo(DeterministicCommandMatchStatus.MATCHED);
        assertThat(match.command().type()).isEqualTo(AgentChatCommandType.STEP_SIMULATION);
        assertThat(match.command().stepCount()).isEqualTo(10);
    }

    @Test
    void matchesPauseCommandDeterministically() {
        DeterministicCommandMatch match = matcher.match(city.getId(), request("pause simulation"));

        assertThat(match.status()).isEqualTo(DeterministicCommandMatchStatus.MATCHED);
        assertThat(match.command().type()).isEqualTo(AgentChatCommandType.PAUSE_SIMULATION);
    }

    @Test
    void matchesMoveToPlaceByHumanName() {
        DeterministicCommandMatch match = matcher.match(city.getId(), request("Tell Elsa to go to the forest"));

        assertThat(match.status()).isEqualTo(DeterministicCommandMatchStatus.MATCHED);
        assertThat(match.command().type()).isEqualTo(AgentChatCommandType.MOVE_TO_PLACE);
        assertThat(match.command().primaryHumanId()).isEqualTo(elsa.getId());
        assertThat(match.command().placeId()).isEqualTo("forest");
    }

    @Test
    void matchesMeetHumanByNames() {
        DeterministicCommandMatch match = matcher.match(city.getId(), request("Tell Pierre to meet Lucas"));

        assertThat(match.status()).isEqualTo(DeterministicCommandMatchStatus.MATCHED);
        assertThat(match.command().type()).isEqualTo(AgentChatCommandType.MEET_HUMAN);
        assertThat(match.command().primaryHumanId()).isEqualTo(pierre.getId());
        assertThat(match.command().secondaryHumanId()).isEqualTo(lucas.getId());
    }

    @Test
    void failsClosedWhenHumanCannotBeResolved() {
        DeterministicCommandMatch match = matcher.match(city.getId(), request("focus on Anna"));

        assertThat(match.status()).isEqualTo(DeterministicCommandMatchStatus.AMBIGUOUS);
        assertThat(match.command()).isNull();
    }

    private AgentChatRequestInput request(String message) {
        AgentChatRequestInput input = new AgentChatRequestInput();
        input.setMessage(message);
        return input;
    }

    private Human saveHuman(String name) {
        Human human = new Human();
        human.setCity(city);
        human.setName(name);
        human.setX(0.5);
        human.setY(0.5);
        return humanRepository.save(human);
    }
}
