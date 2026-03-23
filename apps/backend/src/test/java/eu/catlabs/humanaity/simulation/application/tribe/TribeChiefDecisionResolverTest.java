package eu.catlabs.humanaity.simulation.application.tribe;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.ai.application.AiGenerationContext;
import eu.catlabs.humanaity.ai.application.AiGenerationService;
import eu.catlabs.humanaity.ai.domain.AiCallContextType;
import eu.catlabs.humanaity.ai.domain.AiResponse;
import eu.catlabs.humanaity.simulation.domain.TribeDecisionSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TribeChiefDecisionResolverTest {

    @Mock
    private AiGenerationService aiGenerationService;

    @Test
    void invalidLlmOutputFallsBackToDeterministicPlanSelection() {
        when(aiGenerationService.generate(any(), any()))
                .thenReturn(AiResponse.builder().rawContent("not-json").build());

        TribeChiefDecisionResolver resolver = new TribeChiefDecisionResolver(
                aiGenerationService,
                new ObjectMapper(),
                true
        );

        TribeDecisionCandidate wait = new TribeDecisionCandidate(
                "tribe-a:wait",
                TribeDecisionType.WAIT,
                "tribe-a",
                10L,
                List.of(),
                null,
                null,
                null,
                0,
                "Wait"
        );
        TribeDecisionCandidate sendTwo = new TribeDecisionCandidate(
                "tribe-a:send-two:known-place:11-12",
                TribeDecisionType.SEND_TWO_MEMBERS_TO_KNOWN_PLACE,
                "tribe-a",
                10L,
                List.of(11L, 12L),
                "known-place",
                0.4,
                0.6,
                10,
                "Send two members"
        );

        TribeChiefDecisionResolver.TribeDecisionResolution resolution = resolver.resolve(42L, List.of(wait, sendTwo));

        assertThat(resolution.candidate()).isEqualTo(sendTwo);
        assertThat(resolution.decisionSource()).isEqualTo(TribeDecisionSource.DETERMINISTIC_FALLBACK);
        assertThat(resolution.fallbackReason()).isEqualTo("invalid-json");

        ArgumentCaptor<AiGenerationContext> contextCaptor = ArgumentCaptor.forClass(AiGenerationContext.class);
        verify(aiGenerationService).generate(any(), contextCaptor.capture());
        assertThat(contextCaptor.getValue().contextType()).isEqualTo(AiCallContextType.CHIEF_DECISION);
        assertThat(contextCaptor.getValue().cityId()).isEqualTo(42L);
    }
}
