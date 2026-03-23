package eu.catlabs.humanaity.simulation.application.tribe;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.ai.application.AiGenerationContext;
import eu.catlabs.humanaity.ai.application.AiGenerationService;
import eu.catlabs.humanaity.ai.domain.AiResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidatedLlmTribeDecisionSelectorTest {

    private final AiGenerationService aiGenerationService = mock(AiGenerationService.class);
    private final ValidatedLlmTribeDecisionSelector selector =
            new ValidatedLlmTribeDecisionSelector(aiGenerationService, new ObjectMapper());

    @Test
    void returnsMatchingCandidateWhenFallbackJsonIsValid() {
        when(aiGenerationService.generate(any(), any(AiGenerationContext.class))).thenReturn(AiResponse.builder()
                .rawContent("""
                        {"candidateId":"coord-2"}
                        """)
                .build());

        TribeDecisionCandidate selected = selector.select(List.of(
                candidate("coord-1", 10),
                candidate("coord-2", 20)
        )).orElseThrow();

        assertThat(selected.candidateId()).isEqualTo("coord-2");
    }

    @Test
    void rejectsInvalidOutputAndFallsBackDeterministically() {
        when(aiGenerationService.generate(any(), any(AiGenerationContext.class))).thenReturn(AiResponse.builder()
                .rawContent("""
                        {"candidateId":"does-not-exist"}
                        """)
                .build());

        TribeDecisionCandidate selected = selector.select(List.of(
                candidate("coord-1", 10),
                candidate("coord-2", 20)
        )).orElseThrow();

        assertThat(selected.candidateId()).isEqualTo("coord-2");
    }

    private TribeDecisionCandidate candidate(String id, int priority) {
        return new TribeDecisionCandidate(
                id,
                TribeDecisionType.GROUP_TRAVEL,
                "tribe-a",
                null,
                List.of(1L, 2L),
                "forest",
                0.14,
                0.18,
                priority,
                id
        );
    }
}
