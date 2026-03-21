package eu.catlabs.humanaity.simulation.api;

import eu.catlabs.humanaity.ai.domain.AiEnrichmentStatus;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.invention.domain.Invention;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import eu.catlabs.humanaity.simulation.application.SimulationApplicationService;
import eu.catlabs.humanaity.simulation.application.SimulationCommandService;
import eu.catlabs.humanaity.simulation.application.assistant.SimulationAssistantService;
import eu.catlabs.humanaity.simulation.application.SimulationApplicationService.TimelineHistory;
import eu.catlabs.humanaity.simulation.application.query.SimulationReadModelQueryService;
import eu.catlabs.humanaity.simulation.api.dto.CityOverviewOutput;
import eu.catlabs.humanaity.simulation.api.dto.EventOutput;
import eu.catlabs.humanaity.simulation.api.dto.InventionOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationSnapshotBoundsOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationSnapshotCentroidOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationSnapshotCityOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationSnapshotHumanOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationAssistantRequestInput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationAssistantResponseOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationCommandInput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationCommandOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationKnowledgeOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationSnapshotMetricsOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationSnapshotOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationSnapshotRunOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationTimelineSummaryOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationRunInput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationRunOutput;
import eu.catlabs.humanaity.simulation.api.dto.TimelineOutput;
import eu.catlabs.humanaity.simulation.domain.SimulationRun;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/simulations")
@Tag(name = "Simulations", description = "Simulation management API")
@SecurityRequirement(name = "bearer-jwt")
public class SimulationController {

    private final SimulationApplicationService simulationApplicationService;
    private final SimulationCommandService simulationCommandService;
    private final SimulationAssistantService simulationAssistantService;
    private final UserRepository userRepository;

    public SimulationController(
            SimulationApplicationService simulationApplicationService,
            SimulationCommandService simulationCommandService,
            SimulationAssistantService simulationAssistantService,
            UserRepository userRepository
    ) {
        this.simulationApplicationService = simulationApplicationService;
        this.simulationCommandService = simulationCommandService;
        this.simulationAssistantService = simulationAssistantService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{cityId}")
    @Operation(summary = "Create a simulation run for a city")
    public ResponseEntity<SimulationRunOutput> createRun(
            @PathVariable Long cityId,
            @RequestBody(required = false) SimulationRunInput input
    ) {
        try {
            SimulationRun run = (input != null && input.getSeed() != null)
                    ? simulationApplicationService.createRun(cityId, input.getSeed())
                    : simulationApplicationService.createRun(cityId);
            return ResponseEntity.ok(toOutput(run, cityId));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @GetMapping("/{cityId}")
    @Operation(summary = "Load simulation run metadata for a city")
    public ResponseEntity<SimulationRunOutput> loadRun(@PathVariable Long cityId) {
        try {
            SimulationRun run = simulationApplicationService.loadRun(cityId);
            return ResponseEntity.ok(toOutput(run, cityId));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping("/{cityId}/pause")
    @Operation(summary = "Pause simulation run for a city")
    public ResponseEntity<SimulationRunOutput> pauseRun(@PathVariable Long cityId) {
        try {
            SimulationRun run = simulationApplicationService.pauseRun(cityId);
            return ResponseEntity.ok(toOutput(run, cityId));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping("/{cityId}/resume")
    @Operation(summary = "Resume simulation run for a city")
    public ResponseEntity<SimulationRunOutput> resumeRun(@PathVariable Long cityId) {
        try {
            SimulationRun run = simulationApplicationService.resumeRun(cityId);
            return ResponseEntity.ok(toOutput(run, cityId));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }


    @PostMapping("/{cityId}/step")
    @Operation(summary = "Execute one deterministic simulation step for a city")
    public ResponseEntity<SimulationRunOutput> stepSimulation(@PathVariable Long cityId) {
        try {
            SimulationRun run = simulationApplicationService.step(cityId);
            return ResponseEntity.ok(toOutput(run, cityId));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping("/{cityId}/commands")
    @Operation(summary = "Execute one deterministic simulation command for a city")
    public ResponseEntity<SimulationCommandOutput> executeCommand(
            @PathVariable Long cityId,
            @RequestBody SimulationCommandInput input,
            Authentication authentication
    ) {
        try {
            User currentUser = resolveCurrentUser(authentication);
            return ResponseEntity.ok(simulationCommandService.execute(cityId, currentUser, input));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    @PostMapping("/{cityId}/assistant")
    @Operation(summary = "Query the deterministic simulation assistant for a city")
    public ResponseEntity<SimulationAssistantResponseOutput> queryAssistant(
            @PathVariable Long cityId,
            @RequestBody SimulationAssistantRequestInput input,
            Authentication authentication
    ) {
        try {
            User currentUser = resolveCurrentUser(authentication);
            return ResponseEntity.ok(simulationAssistantService.handle(cityId, currentUser, input));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{cityId}/start")
    @Operation(summary = "Start simulation for a city")
    public ResponseEntity<Map<String, String>> startSimulation(@PathVariable Long cityId) {
        try {
            String message = simulationApplicationService.startSimulation(cityId);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping("/{cityId}/stop")
    @Operation(summary = "Stop simulation for a city")
    public ResponseEntity<Map<String, String>> stopSimulation(@PathVariable Long cityId) {
        String message = simulationApplicationService.stopSimulation(cityId);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @GetMapping("/{cityId}/status")
    @Operation(summary = "Check if simulation is running for a city")
    public ResponseEntity<Map<String, Boolean>> isSimulationRunning(@PathVariable Long cityId) {
        boolean isRunning = simulationApplicationService.isRunning(cityId);
        return ResponseEntity.ok(Map.of("running", isRunning));
    }

    @GetMapping("/{cityId}/history/events")
    @Operation(summary = "List city-scoped deterministic history events ordered by tick and sequence")
    public ResponseEntity<List<EventOutput>> listCityEvents(
            @PathVariable Long cityId,
            @RequestParam(required = false) Long fromTick,
            @RequestParam(required = false) Long toTick,
            @RequestParam(required = false) Integer limit
    ) {
        try {
            validateTickRange(fromTick, toTick);
            List<EventOutput> outputs = simulationApplicationService.listCityEvents(cityId, fromTick, toTick, limit).stream()
                    .map(event -> toEventOutput(cityId, event))
                    .toList();
            return ResponseEntity.ok(outputs);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/{cityId}/history/inventions")
    @Operation(summary = "List city-scoped deterministic inventions ordered by tick and key")
    public ResponseEntity<List<InventionOutput>> listCityInventions(
            @PathVariable Long cityId,
            @RequestParam(required = false) Long fromTick,
            @RequestParam(required = false) Long toTick,
            @RequestParam(required = false) Integer limit
    ) {
        try {
            validateTickRange(fromTick, toTick);
            List<InventionOutput> outputs = simulationApplicationService.listCityInventions(cityId, fromTick, toTick, limit).stream()
                    .map(invention -> toInventionOutput(cityId, invention))
                    .toList();
            return ResponseEntity.ok(outputs);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/{cityId}/history/timeline")
    @Operation(summary = "Get a city-scoped timeline bundle containing ordered events and inventions")
    public ResponseEntity<TimelineOutput> getCityTimeline(
            @PathVariable Long cityId,
            @RequestParam(required = false) Long fromTick,
            @RequestParam(required = false) Long toTick,
            @RequestParam(required = false) Integer limit
    ) {
        try {
            validateTickRange(fromTick, toTick);
            TimelineHistory timeline = simulationApplicationService.listCityTimeline(cityId, fromTick, toTick, limit);
            TimelineOutput output = new TimelineOutput(
                    timeline.cityId(),
                    timeline.fromTick(),
                    timeline.toTick(),
                    timeline.events().size(),
                    timeline.inventions().size(),
                    timeline.events().stream().map(event -> toEventOutput(cityId, event)).toList(),
                    timeline.inventions().stream().map(invention -> toInventionOutput(cityId, invention)).toList()
            );
            return ResponseEntity.ok(output);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/overview")
    @Operation(summary = "List city overviews with backend-owned simulation read-model fields")
    public ResponseEntity<List<CityOverviewOutput>> listCityOverviews() {
        List<CityOverviewOutput> outputs = simulationApplicationService.listCityOverviews().stream()
                .sorted(Comparator.comparing(SimulationReadModelQueryService.CityOverviewProjection::cityId))
                .map(this::toCityOverviewOutput)
                .toList();
        return ResponseEntity.ok(outputs);
    }

    @GetMapping("/{cityId}/snapshot")
    @Operation(summary = "Get a backend-owned simulation snapshot for one city")
    public ResponseEntity<SimulationSnapshotOutput> getCitySnapshot(@PathVariable Long cityId) {
        try {
            return ResponseEntity.ok(toSimulationSnapshotOutput(simulationApplicationService.getCitySnapshot(cityId)));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException();
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(UnauthorizedException::new);
    }

    private void validateTickRange(Long fromTick, Long toTick) {
        if (fromTick != null && fromTick < 0) {
            throw new IllegalArgumentException("fromTick must be >= 0");
        }
        if (toTick != null && toTick < 0) {
            throw new IllegalArgumentException("toTick must be >= 0");
        }
        if (fromTick != null && toTick != null && toTick < fromTick) {
            throw new IllegalArgumentException("toTick must be >= fromTick");
        }
    }

    private SimulationRunOutput toOutput(SimulationRun run, Long cityId) {
        boolean isRunning = simulationApplicationService.isRunning(cityId);
        return new SimulationRunOutput(
                run.getId(),
                cityId,
                run.getSeed(),
                run.getTick(),
                run.getStatus(),
                isRunning,
                run.getCreatedAt(),
                run.getUpdatedAt()
        );
    }

    private EventOutput toEventOutput(Long cityId, Event event) {
        return new EventOutput(
                event.getId(),
                cityId,
                event.getTick(),
                event.getSequenceInTick(),
                event.getEventCategory(),
                event.getEventType(),
                event.getActorIds(),
                event.getPayload(),
                event.getImportance(),
                event.getYear(),
                event.getEra(),
                event.getEventKey(),
                event.getCreatedAt(),
                event.getEnrichmentStatus() != null ? event.getEnrichmentStatus() : AiEnrichmentStatus.NONE,
                event.getEnrichmentFallback() != null ? event.getEnrichmentFallback() : false,
                event.getEnrichedSnippet(),
                event.getEnrichmentProvider(),
                event.getEnrichmentModel(),
                event.getEnrichmentUpdatedAt()
        );
    }

    private InventionOutput toInventionOutput(Long cityId, Invention invention) {
        return new InventionOutput(
                invention.getId(),
                cityId,
                invention.getTickCreated(),
                invention.getCategory(),
                invention.getInventionKey(),
                invention.getTitle(),
                invention.getSummary(),
                invention.getSourceEventKeys(),
                invention.getImpactScore(),
                invention.getYearCreated(),
                invention.getEraCreated(),
                invention.getCreatedAt(),
                invention.getEnrichmentStatus() != null ? invention.getEnrichmentStatus() : AiEnrichmentStatus.NONE,
                invention.getEnrichmentFallback() != null ? invention.getEnrichmentFallback() : false,
                invention.getEnrichedTitle(),
                invention.getEnrichedSummary(),
                invention.getEnrichmentProvider(),
                invention.getEnrichmentModel(),
                invention.getEnrichmentUpdatedAt()
        );
    }

    private CityOverviewOutput toCityOverviewOutput(SimulationReadModelQueryService.CityOverviewProjection projection) {
        return new CityOverviewOutput(
                projection.cityId(),
                projection.cityName(),
                projection.hasRun(),
                projection.runStatus(),
                projection.running(),
                projection.tick(),
                projection.year(),
                projection.era(),
                projection.population(),
                projection.inventionCount(),
                projection.eventCount(),
                projection.discoveryUnlockCount(),
                projection.unlockedInventionCount(),
                projection.applicationUnlockCount(),
                projection.updatedAt()
        );
    }

    private SimulationSnapshotOutput toSimulationSnapshotOutput(SimulationReadModelQueryService.SimulationSnapshotProjection projection) {
        SimulationReadModelQueryService.CityProjection city = projection.city();
        SimulationReadModelQueryService.RunProjection run = projection.run();
        SimulationReadModelQueryService.MetricsProjection metrics = projection.metrics();
        SimulationReadModelQueryService.TimelineSummaryProjection timelineSummary = projection.timelineSummary();

        SimulationSnapshotCentroidOutput centroidOutput = metrics.centroid() == null
                ? null
                : new SimulationSnapshotCentroidOutput(metrics.centroid().x(), metrics.centroid().y());
        SimulationSnapshotBoundsOutput boundsOutput = metrics.bounds() == null
                ? null
                : new SimulationSnapshotBoundsOutput(
                metrics.bounds().minX(),
                metrics.bounds().maxX(),
                metrics.bounds().minY(),
                metrics.bounds().maxY()
        );

        return new SimulationSnapshotOutput(
                new SimulationSnapshotCityOutput(city.id(), city.name()),
                new SimulationSnapshotRunOutput(
                        run.hasRun(),
                        run.runId(),
                        run.seed(),
                        run.status(),
                        run.running(),
                        run.tick(),
                        run.year(),
                        run.era(),
                        run.createdAt(),
                        run.updatedAt()
                ),
                new SimulationTimelineSummaryOutput(
                        timelineSummary.latestEventTick(),
                        timelineSummary.latestInventionTick(),
                        timelineSummary.latestKnowledgeUnlockTick(),
                        timelineSummary.recentEventCount(),
                        timelineSummary.recentInventionCount(),
                        timelineSummary.recentKnowledgeUnlockCount()
                ),
                new SimulationKnowledgeOutput(
                        projection.knowledge().unlockedDiscoveries(),
                        projection.knowledge().unlockedInventions(),
                        projection.knowledge().unlockedApplications()
                ),
                projection.humans().stream()
                        .map(human -> new SimulationSnapshotHumanOutput(
                                human.id(),
                                human.name(),
                                human.tribeId(),
                                human.x(),
                                human.y(),
                                human.busy()
                        ))
                        .toList(),
                new SimulationSnapshotMetricsOutput(
                        metrics.population(),
                        metrics.busyCount(),
                        metrics.busyRatio(),
                        centroidOutput,
                        boundsOutput,
                        metrics.eventCount(),
                        metrics.inventionCount()
                ),
                projection.recentEvents().stream()
                        .map(event -> toEventOutput(city.id(), event))
                        .toList(),
                projection.recentInventions().stream()
                        .map(invention -> toInventionOutput(city.id(), invention))
                        .toList()
        );
    }

    private static class UnauthorizedException extends RuntimeException {
    }
}
