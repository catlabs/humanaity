package eu.catlabs.humanaity.simulation.application.tribe;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class DeterministicTribeDecisionSelector implements TribeDecisionSelector {

    @Override
    public Optional<TribeDecisionCandidate> select(List<TribeDecisionCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        return candidates.stream()
                .sorted(Comparator
                        .comparingInt(TribeDecisionCandidate::priority)
                        .reversed()
                        .thenComparing(TribeDecisionCandidate::candidateId))
                .findFirst();
    }
}
