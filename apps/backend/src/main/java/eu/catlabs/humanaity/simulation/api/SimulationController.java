package eu.catlabs.humanaity.simulation.api;

import eu.catlabs.humanaity.ai.domain.AiEnrichmentStatus;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.invention.domain.Invention;
import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.auth.infrastructure.persistence.UserRepository;
import eu.catlabs.humanaity.infrastructure.web.AbuseProtectionService;
import eu.catlabs.humanaity.infrastructure.web.ApiErrorResponse;
import eu.catlabs.humanaity.infrastructure.web.RateLimitExceededException;
import eu.catlabs.humanaity.simulation.application.SimulationApplicationService;
import eu.catlabs.humanaity.simulation.application.SimulationCommandBuilderService;
import eu.catlabs.humanaity.simulation.application.SimulationCommandService;
import eu.catlabs.humanaity.simulation.application.assistant.SimulationAssistantCommandsCatalog;
import eu.catlabs.humanaity.simulation.application.assistant.SimulationAssistantService;
import eu.catlabs.humanaity.simulation.application.SimulationApplicationService.TimelineHistory;
import eu.catlabs.humanaity.simulation.application.SimulationPlaceRegistry;
import eu.catlabs.humanaity.simulation.application.query.SimulationReadModelQueryService;
import eu.catlabs.humanaity.simulation.api.dto.CityOverviewOutput;
import eu.catlabs.humanaity.simulation.api.dto.EventOutput;
import eu.catlabs.humanaity.simulation.api.dto.InventionOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationSnapshotBoundsOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationSnapshotCentroidOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationSnapshotCityOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationSnapshotHumanOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationAssistantCommandDescriptorOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationAssistantRequestInput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationAssistantResponseOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationCommandInput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationCommandOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationCommandBuilderOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationKnowledgeOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationSnapshotMetricsOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationSnapshotOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationSnapshotRunOutput;
import eu.catlabs.humanaity.simulation.api.dto.TribeHouseOutput;
import eu.catlabs.humanaity.simulation.api.dto.TribeKnownPlaceOutput;
import eu.catlabs.humanaity.simulation.api.dto.TribeSnapshotOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationTimelineSummaryOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationRunInput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationRunOutput;
import eu.catlabs.humanaity.simulation.api.dto.TimelineOutput;
import eu.catlabs.humanaity.simulation.domain.SimulationRun;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@RestController
@RequestMapping("/api/simulations")
@Tag(name = "Simulations", description = "Simulation management API")
@SecurityRequirement(name = "bearer-jwt")
public class SimulationController {

    private final SimulationApplicationService simulationApplicationService;
    private final SimulationCommandBuilderService simulationCommandBuilderService;
    private final SimulationCommandService simulationCommandService;
    private final SimulationAssistantService simulationAssistantService;
    private final SimulationAssistantCommandsCatalog simulationAssistantCommandsCatalog;
    private final UserRepository userRepository;
    private final AbuseProtectionService abuseProtectionService;

    public SimulationController(
            SimulationApplicationService simulationApplicationService,
            SimulationCommandBuilderService simulationCommandBuilderService,
            SimulationCommandService simulationCommandService,
            SimulationAssistantService simulationAssistantService,
            SimulationAssistantCommandsCatalog simulationAssistantCommandsCatalog,
            UserRepository userRepository,
            AbuseProtectionService abuseProtectionService
    ) {
        this.simulationApplicationService = simulationApplicationService;
        this.simulationCommandBuilderService = simulationCommandBuilderService;
        this.simulationCommandService = simulationCommandService;
        this.simulationAssistantService = simulationAssistantService;
        this.simulationAssistantCommandsCatalog = simulationAssistantCommandsCatalog;
        this.userRepository = userRepository;
        this.abuseProtectionService = abuseProtectionService;
    }

    @PostMapping("/{cityId}")
    @Operation(summary = "Create a simulation run for a city")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulation run created",
                    content = @Content(schema = @Schema(implementation = SimulationRunOutput.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<?> createRun(
            @PathVariable Long cityId,
            @RequestBody(required = false) SimulationRunInput input,
            Authentication authentication
    ) {
        try {
            abuseProtectionService.checkSimulationMutation(resolveCurrentSubject(authentication));
            SimulationRun run = (input != null && input.getSeed() != null)
                    ? simulationApplicationService.createRun(cityId, input.getSeed())
                    : simulationApplicationService.createRun(cityId);
            return ResponseEntity.ok(toOutput(run, cityId));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RateLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiErrorResponse(e.getMessage()));
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulation run paused",
                    content = @Content(schema = @Schema(implementation = SimulationRunOutput.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<?> pauseRun(@PathVariable Long cityId, Authentication authentication) {
        try {
            abuseProtectionService.checkSimulationMutation(resolveCurrentSubject(authentication));
            SimulationRun run = simulationApplicationService.pauseRun(cityId);
            return ResponseEntity.ok(toOutput(run, cityId));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RateLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiErrorResponse(e.getMessage()));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping("/{cityId}/resume")
    @Operation(summary = "Resume simulation run for a city")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulation run resumed",
                    content = @Content(schema = @Schema(implementation = SimulationRunOutput.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<?> resumeRun(@PathVariable Long cityId, Authentication authentication) {
        try {
            abuseProtectionService.checkSimulationMutation(resolveCurrentSubject(authentication));
            SimulationRun run = simulationApplicationService.resumeRun(cityId);
            return ResponseEntity.ok(toOutput(run, cityId));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RateLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiErrorResponse(e.getMessage()));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }


    @PostMapping("/{cityId}/step")
    @Operation(summary = "Execute one deterministic simulation step for a city")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulation step executed",
                    content = @Content(schema = @Schema(implementation = SimulationRunOutput.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<?> stepSimulation(@PathVariable Long cityId, Authentication authentication) {
        try {
            abuseProtectionService.checkSimulationMutation(resolveCurrentSubject(authentication));
            SimulationRun run = simulationApplicationService.step(cityId);
            return ResponseEntity.ok(toOutput(run, cityId));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RateLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiErrorResponse(e.getMessage()));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping("/{cityId}/commands")
    @Operation(summary = "Execute one deterministic simulation command for a city")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulation command executed",
                    content = @Content(schema = @Schema(implementation = SimulationCommandOutput.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<?> executeCommand(
            @PathVariable Long cityId,
            @RequestBody SimulationCommandInput input,
            Authentication authentication
    ) {
        try {
            abuseProtectionService.checkSimulationMutation(resolveCurrentSubject(authentication));
            User currentUser = resolveCurrentUser(authentication);
            return ResponseEntity.ok(simulationCommandService.execute(cityId, currentUser, input));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RateLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiErrorResponse(e.getMessage()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{cityId}/command-builder")
    @Operation(summary = "Load builder metadata for structured simulation command/query execution")
    public ResponseEntity<SimulationCommandBuilderOutput> getCommandBuilder(
            @PathVariable Long cityId,
            Authentication authentication
    ) {
        try {
            User currentUser = resolveCurrentUser(authentication);
            return ResponseEntity.ok(simulationCommandBuilderService.load(cityId, currentUser));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{cityId}/assistant")
    @Operation(summary = "Query the deterministic simulation assistant for a city")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulation assistant response",
                    content = @Content(schema = @Schema(implementation = SimulationAssistantResponseOutput.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<?> queryAssistant(
            @PathVariable Long cityId,
            @RequestBody SimulationAssistantRequestInput input,
            Authentication authentication
    ) {
        try {
            abuseProtectionService.checkSimulationAssistant(resolveCurrentSubject(authentication));
            User currentUser = resolveCurrentUser(authentication);
            return ResponseEntity.ok(simulationAssistantService.handle(cityId, currentUser, input));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RateLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiErrorResponse(e.getMessage()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{cityId}/start")
    @Operation(summary = "Start simulation for a city")
    public ResponseEntity<?> startSimulation(@PathVariable Long cityId, Authentication authentication) {
        try {
            abuseProtectionService.checkSimulationMutation(resolveCurrentSubject(authentication));
            String message = simulationApplicationService.startSimulation(cityId);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RateLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiErrorResponse(e.getMessage()));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping("/{cityId}/stop")
    @Operation(summary = "Stop simulation for a city")
    public ResponseEntity<?> stopSimulation(@PathVariable Long cityId, Authentication authentication) {
        try {
            abuseProtectionService.checkSimulationMutation(resolveCurrentSubject(authentication));
            String message = simulationApplicationService.stopSimulation(cityId);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RateLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiErrorResponse(e.getMessage()));
        }
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

    @GetMapping("/assistant/commands")
    @Operation(summary = "List deterministic simulation assistant commands")
    public ResponseEntity<List<SimulationAssistantCommandDescriptorOutput>> listAssistantCommands(
            Authentication authentication
    ) {
        try {
            resolveCurrentUser(authentication);
            return ResponseEntity.ok(simulationAssistantCommandsCatalog.listSupportedCommands());
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
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

    private String resolveCurrentSubject(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException();
        }
        return authentication.getName();
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
                eventTitle(event),
                eventSummary(cityId, event),
                event.getCreatedAt(),
                event.getEnrichmentStatus() != null ? event.getEnrichmentStatus() : AiEnrichmentStatus.NONE,
                event.getEnrichmentFallback() != null ? event.getEnrichmentFallback() : false,
                event.getEnrichedSnippet(),
                event.getEnrichmentProvider(),
                event.getEnrichmentModel(),
                event.getEnrichmentUpdatedAt()
        );
    }

    private String eventTitle(Event event) {
        String payloadTitle = payloadValue(event, "title");
        if (!payloadTitle.isBlank()) {
            return payloadTitle;
        }

        return switch (event.getEventType()) {
            case SIMULATION_STARTED -> "Simulation started";
            case SIMULATION_PAUSED -> "Simulation paused";
            case SIMULATION_RESUMED -> "Simulation resumed";
            case SIMULATION_COMPLETED -> "Simulation completed";
            case HUMANS_COLLIDED -> "Human encounter";
            case GOAL_ASSIGNED -> "Internal goal assigned";
            case GOAL_COMPLETED -> "Internal goal completed";
            case HUMAN_ACTION_PERFORMED -> "Human action";
            case DISCOVERY_UNLOCKED -> "Discovery unlocked";
            case DIALOGUE_EXCHANGED -> "Human interaction";
            case INVENTION_EMERGED -> "Invention emerged";
            case TRIBE_PLACE_DISCOVERED -> "Tribe place discovered";
            case TRIBE_DISCOVERY_REPORTED -> "Tribe discovery reported";
            case TRIBE_SCOUT_REPORT -> "Scout report";
            case TRIBE_PLAN_CHOSEN -> "Tribe plan chosen";
            case TRIBE_GROUP_TRAVEL_COORDINATED -> "Coordinated tribe travel";
        };
    }

    private String eventSummary(Long cityId, Event event) {
        String payloadSummary = payloadValue(event, "summary");
        if (!payloadSummary.isBlank()) {
            return payloadSummary;
        }

        return switch (event.getEventType()) {
            case SIMULATION_STARTED -> "The simulation started.";
            case SIMULATION_PAUSED -> "The simulation was paused.";
            case SIMULATION_RESUMED -> "The simulation resumed.";
            case SIMULATION_COMPLETED -> "The simulation ended.";
            case HUMANS_COLLIDED, DIALOGUE_EXCHANGED -> summarizeInteraction(event, cityId);
            case GOAL_ASSIGNED -> "A human received an internal goal.";
            case GOAL_COMPLETED -> "A human completed an internal goal.";
            case HUMAN_ACTION_PERFORMED -> summarizeAction(event);
            case DISCOVERY_UNLOCKED -> summarizeDiscovery(event);
            case INVENTION_EMERGED -> summarizeInvention(event);
            case TRIBE_PLACE_DISCOVERED -> summarizeTribeDiscovery(event);
            case TRIBE_DISCOVERY_REPORTED -> summarizeTribeReport(event);
            case TRIBE_SCOUT_REPORT -> "A scout returned to share a tribe report.";
            case TRIBE_PLAN_CHOSEN -> "A tribe selected a new current plan.";
            case TRIBE_GROUP_TRAVEL_COORDINATED -> summarizeTribeTravel(event);
        };
    }

    private String summarizeInteraction(Event event, Long cityId) {
        int count = event.getActorIds() == null ? 0 : event.getActorIds().size();
        String participants = describeParticipantCount(count);
        String interactionKind = payloadValue(event, "interactionType");
        String placeLabel = readablePlaceLabel(payloadValue(event, "placeId"));

        if ("GROUP_CONVERSATION".equals(interactionKind) || count >= 3) {
            return placeLabel == null
                    ? participants + " shared a group interaction."
                    : participants + " shared a group interaction near " + placeLabel + ".";
        }

        if (placeLabel != null) {
            return participants + " met near " + placeLabel + ".";
        }

        if (!payloadValue(event, "trigger").isBlank()) {
            return participants + " met and interacted.";
        }

        return "A human interaction occurred.";
    }

    private String summarizeDiscovery(Event event) {
        String placeLabel = readablePlaceLabel(payloadValue(event, "placeId"));
        String trigger = payloadValue(event, "trigger");
        if ("PROXIMITY_GROUP".equals(trigger)) {
            return placeLabel == null
                    ? "A recurring interaction unlocked a discovery."
                    : "A recurring interaction near " + placeLabel + " unlocked a discovery.";
        }
        if (placeLabel != null) {
            return "A discovery emerged from " + placeLabel + ".";
        }
        return "A discovery emerged from the interaction.";
    }

    private String summarizeAction(Event event) {
        String actionType = payloadValue(event, "actionType");
        if (!actionType.isBlank()) {
            return "A human performed " + formatLabel(actionType) + ".";
        }
        return "A human performed a meaningful action.";
    }

    private String summarizeInvention(Event event) {
        String title = payloadValue(event, "title");
        if (!title.isBlank()) {
            return title + " emerged from prior discoveries.";
        }
        return "An invention emerged from prior discoveries.";
    }

    private String summarizeTribeDiscovery(Event event) {
        String tribeId = payloadValue(event, "tribeId");
        String placeId = readablePlaceLabel(payloadValue(event, "placeId"));
        if (!tribeId.isBlank() && placeId != null) {
            return tribeId + " discovered " + placeId + ".";
        }
        return "A tribe discovered a new place.";
    }

    private String summarizeTribeReport(Event event) {
        String tribeId = payloadValue(event, "tribeId");
        String placeId = readablePlaceLabel(payloadValue(event, "placeId"));
        if (!tribeId.isBlank() && placeId != null) {
            return tribeId + " reported " + placeId + " to the tribe.";
        }
        return "A tribe reported a discovery.";
    }

    private String summarizeTribeScoutReport(Event event) {
        String tribeId = payloadValue(event, "tribeId");
        String chiefId = payloadValue(event, "chiefId");
        String placeId = readablePlaceLabel(payloadValue(event, "placeId"));
        if (!tribeId.isBlank() && !chiefId.isBlank() && placeId != null) {
            return tribeId + " scout reported " + placeId + " to chief " + chiefId + ".";
        }
        if (!tribeId.isBlank() && placeId != null) {
            return tribeId + " scout reported " + placeId + ".";
        }
        return "A scout reported a discovery.";
    }

    private String summarizeTribePlanChosen(Event event) {
        String tribeId = payloadValue(event, "tribeId");
        String planType = payloadValue(event, "planType");
        String placeId = readablePlaceLabel(payloadValue(event, "placeId"));
        String assignees = payloadValue(event, "assigneeIds");
        if (!tribeId.isBlank() && !planType.isBlank() && placeId != null) {
            return tribeId + " chose " + formatLabel(planType) + " toward " + placeId + " for " + assignees + ".";
        }
        if (!tribeId.isBlank() && !planType.isBlank()) {
            return tribeId + " chose " + formatLabel(planType) + ".";
        }
        return "A tribe selected a plan.";
    }

    private String summarizeTribeTravel(Event event) {
        String tribeId = payloadValue(event, "tribeId");
        String placeId = readablePlaceLabel(payloadValue(event, "placeId"));
        String members = payloadValue(event, "memberIds");
        if (!tribeId.isBlank() && placeId != null) {
            return tribeId + " coordinated travel to " + placeId + " with members " + members + ".";
        }
        return "A tribe coordinated travel.";
    }

    private String payloadValue(Event event, String key) {
        if (event.getPayload() == null) {
            return "";
        }
        return event.getPayload().getOrDefault(key, "").trim();
    }

    private String readablePlaceLabel(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            return null;
        }
        return SimulationPlaceRegistry.byId(placeId)
                .map(place -> formatLabel(place.id()))
                .orElseGet(() -> formatLabel(placeId));
    }

    private String formatLabel(String value) {
        String[] parts = value.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            String lower = part.toLowerCase(Locale.ROOT);
            builder.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
        }
        return builder.toString();
    }

    private String describeParticipantCount(int count) {
        return switch (count) {
            case 0 -> "Humans";
            case 1 -> "One human";
            case 2 -> "Two humans";
            case 3 -> "Three humans";
            default -> count + " humans";
        };
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
                projection.tribes().stream()
                        .map(tribe -> new TribeSnapshotOutput(
                                tribe.tribeId(),
                                new TribeHouseOutput(tribe.house().x(), tribe.house().y()),
                                tribe.scoutHumanId(),
                                tribe.knownPlaces().stream()
                                        .map(place -> new TribeKnownPlaceOutput(
                                                place.placeId(),
                                                place.discoveredByHumanId(),
                                                place.discoveredTick(),
                                                place.reportedTick(),
                                                place.reported()
                                        ))
                                        .toList()
                        ))
                        .toList(),
                projection.humans().stream()
                        .map(human -> new SimulationSnapshotHumanOutput(
                                human.id(),
                                human.name(),
                                human.tribeId(),
                                human.tribeRole(),
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
