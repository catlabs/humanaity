package eu.catlabs.humanaity.city.api;

import eu.catlabs.humanaity.city.api.dto.CityInput;
import eu.catlabs.humanaity.city.api.dto.CityOutput;
import eu.catlabs.humanaity.city.application.CityApplicationService;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import eu.catlabs.humanaity.infrastructure.web.AbuseProtectionService;
import eu.catlabs.humanaity.infrastructure.web.ApiErrorResponse;
import eu.catlabs.humanaity.infrastructure.web.RateLimitExceededException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cities")
@Tag(name = "Cities", description = "City management API")
@SecurityRequirement(name = "bearer-jwt")
public class CityController {
    private final CityApplicationService cityApplicationService;
    private final UserRepository userRepository;
    private final AbuseProtectionService abuseProtectionService;

    public CityController(
            CityApplicationService cityApplicationService,
            UserRepository userRepository,
            AbuseProtectionService abuseProtectionService
    ) {
        this.cityApplicationService = cityApplicationService;
        this.userRepository = userRepository;
        this.abuseProtectionService = abuseProtectionService;
    }

    @GetMapping
    @Operation(summary = "Get all cities")
    public ResponseEntity<List<CityOutput>> getAllCities() {
        List<City> cities = cityApplicationService.getAllCities();
        List<CityOutput> outputs = cities.stream()
                .map(this::toCityOutput)
                .collect(Collectors.toList());
        return ResponseEntity.ok(outputs);
    }

    @GetMapping("/mine")
    @Operation(summary = "Get all cities available to the current authenticated user (legacy alias)")
    public ResponseEntity<List<CityOutput>> getMyCities(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<City> cities = cityApplicationService.getCitiesForUser(resolveCurrentUser(authentication));
        List<CityOutput> outputs = cities.stream()
                .map(this::toCityOutput)
                .collect(Collectors.toList());
        return ResponseEntity.ok(outputs);
    }

    @GetMapping("/search")
    @Operation(summary = "Search cities by name")
    public ResponseEntity<List<CityOutput>> getCitiesByName(@RequestParam String name) {
        List<City> cities = cityApplicationService.getCitiesByName(name);
        List<CityOutput> outputs = cities.stream()
                .map(this::toCityOutput)
                .collect(Collectors.toList());
        return ResponseEntity.ok(outputs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get city by ID")
    public ResponseEntity<CityOutput> getCityById(@PathVariable String id) {
        return cityApplicationService.getCityById(id)
                .map(this::toCityOutput)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new city with generated humans")
    public ResponseEntity<?> createCity(@Valid @RequestBody CityInput input, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            abuseProtectionService.checkCityCreate(authentication.getName());
            User currentUser = resolveCurrentUser(authentication);
            City city = cityApplicationService.createCityForUser(input, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(toCityOutput(city));
        } catch (RateLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a city")
    public ResponseEntity<CityOutput> updateCity(
            @PathVariable Long id,
            @Valid @RequestBody CityInput input,
            Authentication authentication
    ) {
        try {
            User currentUser = resolveCurrentUser(authentication);
            City city = cityApplicationService.updateCity(id, input, currentUser);
            return ResponseEntity.ok(toCityOutput(city));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a city")
    public ResponseEntity<Void> deleteCity(@PathVariable Long id, Authentication authentication) {
        try {
            User currentUser = resolveCurrentUser(authentication);
            cityApplicationService.deleteCity(id, currentUser);
            return ResponseEntity.noContent().build();
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(UnauthorizedException::new);
    }

    private CityOutput toCityOutput(City city) {
        CityOutput output = new CityOutput();
        output.setId(city.getId());
        output.setName(city.getName());
        // Humans will be loaded separately if needed via /api/humans/city/{cityId}
        output.setHumans(null);
        return output;
    }

    private static class UnauthorizedException extends RuntimeException {
    }
}
