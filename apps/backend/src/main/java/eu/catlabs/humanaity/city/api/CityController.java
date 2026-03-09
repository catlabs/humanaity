package eu.catlabs.humanaity.city.api;

import eu.catlabs.humanaity.city.api.dto.CityInput;
import eu.catlabs.humanaity.city.api.dto.CityOutput;
import eu.catlabs.humanaity.city.application.CityApplicationService;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    public CityController(CityApplicationService cityApplicationService, UserRepository userRepository) {
        this.cityApplicationService = cityApplicationService;
        this.userRepository = userRepository;
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
    @Operation(summary = "Get all cities owned by the current user")
    public ResponseEntity<List<CityOutput>> getMyCities(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));

        List<City> cities = cityApplicationService.getCitiesForUser(currentUser);
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
    public ResponseEntity<CityOutput> createCity(@Valid @RequestBody CityInput input, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));

        City city = cityApplicationService.createCityForUser(input, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(toCityOutput(city));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a city")
    public ResponseEntity<CityOutput> updateCity(@PathVariable String id, @Valid @RequestBody CityInput input) {
        try {
            City city = cityApplicationService.updateCity(id, input);
            return ResponseEntity.ok(toCityOutput(city));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a city")
    public ResponseEntity<Void> deleteCity(@PathVariable String id) {
        try {
            cityApplicationService.deleteCity(Long.parseLong(id));
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private CityOutput toCityOutput(City city) {
        CityOutput output = new CityOutput();
        output.setId(city.getId());
        output.setName(city.getName());
        // Humans will be loaded separately if needed via /api/humans/city/{cityId}
        output.setHumans(null);
        return output;
    }
}
