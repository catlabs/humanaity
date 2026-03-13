package eu.catlabs.humanaity.ai.application.enrichment;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.catlabs.humanaity.ai.application.AiGenerationService;
import eu.catlabs.humanaity.ai.application.prompt.EventDialogueEnrichmentPrompt;
import eu.catlabs.humanaity.ai.application.prompt.InventionEnrichmentPrompt;
import eu.catlabs.humanaity.ai.domain.AiEnrichmentStatus;
import eu.catlabs.humanaity.ai.domain.AiProvider;
import eu.catlabs.humanaity.ai.domain.AiResponse;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.domain.EventCategory;
import eu.catlabs.humanaity.event.domain.EventType;
import eu.catlabs.humanaity.history.domain.HistoryEra;
import eu.catlabs.humanaity.invention.domain.Invention;
import eu.catlabs.humanaity.invention.domain.InventionCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiHistoryEnrichmentServiceTest {

    @Mock
    private AiGenerationService aiGenerationService;

    private AiHistoryEnrichmentService service;

    @BeforeEach
    void setUp() {
        service = new AiHistoryEnrichmentService(
                aiGenerationService,
                new InventionEnrichmentPrompt(),
                new EventDialogueEnrichmentPrompt(),
                new ObjectMapper()
        );
    }

    @Test
    void inventionFallsBackWhenAiFails() {
        Invention invention = new Invention();
        invention.setTitle("Canal Irrigation");
        invention.setSummary("Canals move water between crop zones.");
        invention.setInventionKey("DISCOVERY_1");
        invention.setCategory(InventionCategory.TECHNIQUE);
        invention.setImpactScore(42);
        invention.setYearCreated(3);
        invention.setEraCreated(HistoryEra.FOUNDING);

        when(aiGenerationService.generate(any())).thenThrow(new RuntimeException("AI unavailable"));

        service.enrichInvention(invention);

        assertThat(invention.getEnrichmentStatus()).isEqualTo(AiEnrichmentStatus.FALLBACK);
        assertThat(invention.getEnrichmentFallback()).isTrue();
        assertThat(invention.getEnrichedSummary()).startsWith("Fallback:");
    }

    @Test
    void dialogueEventUsesReadyStatusOnValidJson() {
        Event event = new Event();
        event.setEventType(EventType.DIALOGUE_EXCHANGED);
        event.setEventCategory(EventCategory.DIALOGUE);
        event.setEventKey("DIALOGUE_EXCHANGED:12:20001");
        event.setTick(12L);
        event.setYear(2);
        event.setEra(HistoryEra.FOUNDING);
        event.setActorIds(List.of(1L, 2L));
        event.setPayload(Map.of("dialogueKey", "1-2-12"));

        when(aiGenerationService.generate(any())).thenReturn(
                AiResponse.builder()
                        .rawContent("{\"snippet\":\"Two neighbors exchanged methods for grain storage.\"}")
                        .provider(AiProvider.OPENAI)
                        .build()
        );

        service.enrichEventDialogueIfEligible(event);

        assertThat(event.getEnrichmentStatus()).isEqualTo(AiEnrichmentStatus.READY);
        assertThat(event.getEnrichmentFallback()).isFalse();
        assertThat(event.getEnrichedSnippet()).contains("grain storage");
    }
}
