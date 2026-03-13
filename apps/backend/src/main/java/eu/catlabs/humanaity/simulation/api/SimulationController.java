package eu.catlabs.humanaity.simulation.api;

import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.invention.domain.Invention;
import eu.catlabs.humanaity.simulation.application.SimulationApplicationService;
import eu.catlabs.humanaity.simulation.application.SimulationApplicationService.TimelineHistory;
import eu.catlabs.humanaity.simulation.api.dto.EventOutput;
import eu.catlabs.humanaity.simulation.api.dto.InventionOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationRunInput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationRunOutput;
import eu.catlabs.humanaity.simulation.api.dto.TimelineOutput;
import eu.catlabs.humanaity.simulation.domain.SimulationRun;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/simulations")
@Tag(name = "Simulations", description = "Simulation management API")
public class SimulationController {

    private final SimulationApplicationService simulationApplicationService;

    public SimulationController(SimulationApplicationService simulationApplicationService) {
        this.simulationApplicationService = simulationApplicationService;
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
            return ResponseEntity.ok(toOutput(run));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @GetMapping("/{cityId}")
    @Operation(summary = "Load simulation run metadata for a city")
    public ResponseEntity<SimulationRunOutput> loadRun(@PathVariable Long cityId) {
        try {
            SimulationRun run = simulationApplicationService.loadRun(cityId);
            return ResponseEntity.ok(toOutput(run));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping("/{cityId}/pause")
    @Operation(summary = "Pause simulation run for a city")
    public ResponseEntity<SimulationRunOutput> pauseRun(@PathVariable Long cityId) {
        try {
            SimulationRun run = simulationApplicationService.pauseRun(cityId);
            return ResponseEntity.ok(toOutput(run));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping("/{cityId}/resume")
    @Operation(summary = "Resume simulation run for a city")
    public ResponseEntity<SimulationRunOutput> resumeRun(@PathVariable Long cityId) {
        try {
            SimulationRun run = simulationApplicationService.resumeRun(cityId);
            return ResponseEntity.ok(toOutput(run));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }


    @PostMapping("/{cityId}/step")
    @Operation(summary = "Execute one deterministic simulation step for a city")
    public ResponseEntity<SimulationRunOutput> stepSimulation(@PathVariable Long cityId) {
        try {
            SimulationRun run = simulationApplicationService.step(cityId);
            return ResponseEntity.ok(toOutput(run));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
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

    private SimulationRunOutput toOutput(SimulationRun run) {
        boolean isRunning = simulationApplicationService.isRunning(run.getCity().getId());
        return new SimulationRunOutput(
                run.getId(),
                run.getCity().getId(),
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
                event.getCreatedAt()
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
                invention.getCreatedAt()
        );
    }
}
