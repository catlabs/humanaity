package eu.catlabs.humanaity.simulation.application;

import eu.catlabs.humanaity.invention.domain.InventionCategory;

import java.util.List;
import java.util.Optional;

public final class SimulationPlaceRegistry {

    private static final List<SimulationPlace> PLACES = List.of(
            new SimulationPlace("forest", 0.14, 0.18, 0.08, InventionCategory.KNOWLEDGE),
            new SimulationPlace("river", 0.82, 0.22, 0.08, InventionCategory.KNOWLEDGE),
            new SimulationPlace("church", 0.52, 0.30, 0.08, InventionCategory.SOCIAL_PRACTICE),
            new SimulationPlace("campfire", 0.34, 0.72, 0.08, InventionCategory.TECHNIQUE),
            new SimulationPlace("house", 0.72, 0.74, 0.08, InventionCategory.TECHNIQUE)
    );

    private SimulationPlaceRegistry() {
    }

    public static List<SimulationPlace> all() {
        return PLACES;
    }

    public static Optional<SimulationPlace> byId(String id) {
        return PLACES.stream()
                .filter(place -> place.id().equals(id))
                .findFirst();
    }

    public record SimulationPlace(
            String id,
            double x,
            double y,
            double radius,
            InventionCategory category
    ) {
    }
}
