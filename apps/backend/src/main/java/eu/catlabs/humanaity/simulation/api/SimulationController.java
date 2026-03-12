package eu.catlabs.humanaity.simulation.api;

import eu.catlabs.humanaity.simulation.application.SimulationApplicationService;
import eu.catlabs.humanaity.simulation.api.dto.SimulationRunInput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationRunOutput;
import eu.catlabs.humanaity.simulation.domain.SimulationRun;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
}
