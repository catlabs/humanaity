package eu.catlabs.humanaity.auth.application;

import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bootstrap-user-init;MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key",
        "humanaity.auth.bootstrap.users[0].email=bootstrap-admin@example.com",
        "humanaity.auth.bootstrap.users[0].password=Test1234!",
        "humanaity.auth.bootstrap.users[0].roles[0]=ROLE_ADMIN",
        "humanaity.auth.bootstrap.users[0].roles[1]=ROLE_USER"
})
class BootstrapUserInitializerTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createsConfiguredBootstrapUsersAtStartup() {
        User user = userRepository.findByEmail("bootstrap-admin@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("Test1234!", user.getPassword())).isTrue();
        assertThat(user.getRoles()).contains("ROLE_ADMIN", "ROLE_USER");
    }
}
