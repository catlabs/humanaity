package eu.catlabs.humanaity.auth.application;

import eu.catlabs.humanaity.auth.api.dto.AuthRequest;
import eu.catlabs.humanaity.auth.api.dto.AuthResponse;
import eu.catlabs.humanaity.auth.api.dto.SignupRequest;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.domain.RefreshToken;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import eu.catlabs.humanaity.auth.infrastructure.persistence.RefreshTokenRepository;
import eu.catlabs.humanaity.auth.infrastructure.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    public AuthResponse signup(SignupRequest request) {
        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = generateAndSaveRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken);
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
