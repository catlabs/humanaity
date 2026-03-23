package eu.catlabs.humanaity.simulation.application.tribe;

import java.util.List;

public record TribeDecisionCandidate(
        String candidateId,
        TribeDecisionType type,
        String tribeId,
        Long humanId,
        List<Long> memberIds,
        String placeId,
        Double targetX,
        Double targetY,
        int priority,
        String description
) {
}
