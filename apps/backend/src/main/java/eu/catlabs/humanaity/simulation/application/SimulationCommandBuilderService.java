package eu.catlabs.humanaity.simulation.application;

import eu.catlabs.humanaity.auth.domain.User;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.human.application.HumanDisplayNameFormatter;
import eu.catlabs.humanaity.human.domain.Human;
import eu.catlabs.humanaity.human.infrastructure.persistence.HumanRepository;
import eu.catlabs.humanaity.simulation.api.dto.SimulationCommandBuilderActionOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationCommandBuilderOptionOutput;
import eu.catlabs.humanaity.simulation.api.dto.SimulationCommandBuilderOutput;
import eu.catlabs.humanaity.simulation.application.assistant.SimulationAssistantCommandsCatalog;
import eu.catlabs.humanaity.simulation.api.dto.SimulationAssistantCommandDescriptorOutput;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class SimulationCommandBuilderService {

    private final CityRepository cityRepository;
    private final HumanRepository humanRepository;
    private final SimulationAssistantCommandsCatalog simulationAssistantCommandsCatalog;

    public SimulationCommandBuilderService(
            CityRepository cityRepository,
            HumanRepository humanRepository,
            SimulationAssistantCommandsCatalog simulationAssistantCommandsCatalog
    ) {
        this.cityRepository = cityRepository;
        this.humanRepository = humanRepository;
        this.simulationAssistantCommandsCatalog = simulationAssistantCommandsCatalog;
    }

    public SimulationCommandBuilderOutput load(Long cityId, User currentUser) {
        cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + cityId));

        List<SimulationCommandBuilderOptionOutput> humans = humanRepository.findByCityIdOrderByIdAsc(cityId).stream()
                .map(this::toHumanOption)
                .toList();

        List<SimulationCommandBuilderOptionOutput> places = SimulationPlaceRegistry.all().stream()
                .map(place -> new SimulationCommandBuilderOptionOutput(place.id(), labelForPlace(place.id())))
                .toList();

        List<SimulationCommandBuilderActionOutput> actions = new ArrayList<>();
        for (SimulationAssistantCommandDescriptorOutput descriptor : simulationAssistantCommandsCatalog.listSupportedCommands()) {
            actions.add(new SimulationCommandBuilderActionOutput(
                    descriptor.commandType(),
                    descriptor.label(),
                    "QUERY",
                    "NONE",
                    "NONE",
                    descriptor.canonicalText(),
                    null,
                    false,
                    List.of()
            ));
        }
        actions.add(new SimulationCommandBuilderActionOutput(
                "FOCUS_HUMAN",
                "Focus human",
                "COMMAND",
                "HUMAN",
                "NONE",
                null,
                "focus",
                false,
                List.of()
        ));
        actions.add(new SimulationCommandBuilderActionOutput(
                "MOVE_HUMAN_TO_PLACE",
                "Go to place",
                "COMMAND",
                "HUMAN",
                "PLACE",
                null,
                "move",
                false,
                places
        ));
        actions.add(new SimulationCommandBuilderActionOutput(
                "MEET_HUMAN",
                "Meet human",
                "COMMAND",
                "HUMAN",
                "HUMAN",
                null,
                "meet",
                true,
                humans
        ));

        return new SimulationCommandBuilderOutput(humans, actions);
    }

    private SimulationCommandBuilderOptionOutput toHumanOption(Human human) {
        return new SimulationCommandBuilderOptionOutput(
                String.valueOf(human.getId()),
                HumanDisplayNameFormatter.displayName(human)
        );
    }

    private String labelForPlace(String placeId) {
        String[] parts = placeId.split("_");
        StringBuilder label = new StringBuilder();
        for (String part : parts) {
            if (label.length() > 0) {
                label.append(' ');
            }
            if (part.isBlank()) {
                continue;
            }
            String lower = part.toLowerCase(Locale.ROOT);
            label.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
        }
        return label.toString();
    }
}
