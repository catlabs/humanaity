package eu.catlabs.humanaity.simulation.application.tribe;

import java.util.List;
import java.util.Optional;

public interface TribeDecisionSelector {
    Optional<TribeDecisionCandidate> select(List<TribeDecisionCandidate> candidates);
}
