package eu.catlabs.humanaity.auth.application;

import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapUserInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapUserInitializer.class);

    private final BootstrapUsersProperties bootstrapUsersProperties;
    private final AuthApplicationService authApplicationService;
    private final UserRepository userRepository;

    public BootstrapUserInitializer(
            BootstrapUsersProperties bootstrapUsersProperties,
            AuthApplicationService authApplicationService,
            UserRepository userRepository
    ) {
        this.bootstrapUsersProperties = bootstrapUsersProperties;
        this.authApplicationService = authApplicationService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (BootstrapUsersProperties.BootstrapUser bootstrapUser : bootstrapUsersProperties.getUsers()) {
            String email = normalize(bootstrapUser.getEmail());
            String password = bootstrapUser.getPassword();
            if (email == null && (password == null || password.isBlank())) {
                continue;
            }
            if (email == null || password == null || password.isBlank()) {
                throw new IllegalStateException("Each bootstrap user must define non-empty email and password");
            }
            if (userRepository.existsByEmail(email)) {
                logger.info("Bootstrap user already exists for {}", email);
                continue;
            }
            authApplicationService.createUser(email, password, bootstrapUser.getRoles());
            logger.info("Created bootstrap user {}", email);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.isEmpty() ? null : normalized;
    }
}
