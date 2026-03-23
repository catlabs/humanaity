package eu.catlabs.humanaity.auth.application;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.catlabs.humanaity.auth.api.dto.AuthRequest;
import eu.catlabs.humanaity.auth.api.dto.AuthResponse;
import eu.catlabs.humanaity.auth.domain.RefreshToken;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.RefreshTokenRepository;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import eu.catlabs.humanaity.auth.infrastructure.security.JwtService;

@Service
public class AuthApplicationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    public AuthApplicationService(UserRepository userRepository,
                                  RefreshTokenRepository refreshTokenRepository,
                                  PasswordEncoder passwordEncoder,
                                  JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public User createUser(String email, String rawPassword, Set<String> roles) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        if (roles != null && !roles.isEmpty()) {
            user.setRoles(new LinkedHashSet<>(roles));
        }
        return userRepository.save(user);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = generateAndSaveRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenString) {
        // Validate refresh token
        if (!jwtService.validateToken(refreshTokenString) || !jwtService.isRefreshToken(refreshTokenString)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        // Find refresh token in database
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // Check if token is expired
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("Refresh token expired");
        }

        User user = refreshToken.getUser();

        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(user.getEmail());
        String newRefreshToken = generateAndSaveRefreshToken(user);

        // Delete old refresh token
        refreshTokenRepository.delete(refreshToken);

        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    private String generateAndSaveRefreshToken(User user) {
        // Delete old refresh tokens for this user
        refreshTokenRepository.deleteByUserId(user.getId());

        // Generate new refresh token
        String jwtRefreshToken = jwtService.generateRefreshToken(user.getEmail());

        // Save refresh token to database
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(jwtRefreshToken);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000));
        refreshTokenRepository.save(refreshToken);

        return jwtRefreshToken;
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
    }
}
