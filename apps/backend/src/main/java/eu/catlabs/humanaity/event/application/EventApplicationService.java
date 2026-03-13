package eu.catlabs.humanaity.event.application;

import eu.catlabs.humanaity.ai.application.enrichment.AiHistoryEnrichmentService;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.domain.EventCategory;
import eu.catlabs.humanaity.event.domain.EventType;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.history.domain.HistoryTimelineMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EventApplicationService {

    private static final int SEQUENCE_BUCKET_SIZE = 10_000;
    private static final int MAX_QUERY_LIMIT = 1_000;
    private static final int DEFAULT_QUERY_LIMIT = 200;

    private final EventRepository eventRepository;
    private final CityRepository cityRepository;
    private final AiHistoryEnrichmentService aiHistoryEnrichmentService;

    public EventApplicationService(
            EventRepository eventRepository,
            CityRepository cityRepository,
            AiHistoryEnrichmentService aiHistoryEnrichmentService
    ) {
        this.eventRepository = eventRepository;
        this.cityRepository = cityRepository;
        this.aiHistoryEnrichmentService = aiHistoryEnrichmentService;
    }

    @Transactional
    public Event emitLifecycleEvent(
            Long cityId,
            long tick,
            EventType eventType,
            List<Long> actorIds,
            Map<String, String> payload,
            int importance
    ) {
        if (eventType.getCategory() != EventCategory.LIFECYCLE) {
            throw new IllegalArgumentException("Lifecycle emission only accepts lifecycle event types");
        }
        List<Event> persisted = emitEventsAtTick(
                cityId,
                tick,
                List.of(new EventDraft(eventType, actorIds, payload, importance, eventType.name()))
        );
        return persisted.get(0);
    }

    @Transactional
    public List<Event> emitEventsAtTick(Long cityId, long tick, List<EventDraft> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            return List.of();
        }

        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + cityId));

        List<Event> persisted = new ArrayList<>();

        for (EventCategory category : EventCategory.values()) {
            List<EventDraft> categoryDrafts = drafts.stream()
                    .filter(draft -> draft.eventCategory() == category)
                    .sorted(eventDraftComparator())
                    .toList();
            if (categoryDrafts.isEmpty()) {
                continue;
            }

            int sequenceInCategory = nextSequenceInCategory(cityId, tick, category);
            for (EventDraft draft : categoryDrafts) {
                Event event = new Event();
                event.setCity(city);
                event.setTick(tick);
                event.setSequenceInTick(sequenceInCategory++);
                event.setEventCategory(category);
                event.setEventType(draft.eventType());
                event.setActorIds(draft.actorIds());
                event.setPayload(draft.payload());
                event.setImportance(Math.max(0, draft.importance()));
                event.setYear(HistoryTimelineMapper.yearForTick(tick));
                event.setEra(HistoryTimelineMapper.eraForTick(tick));
                event.setEventKey(buildEventKey(draft.eventType(), tick, event.getSequenceInTick()));
                persisted.add(event);
            }
        }

        List<Event> saved = eventRepository.saveAll(persisted);
        saved.forEach(aiHistoryEnrichmentService::enrichEventDialogueIfEligible);
        saved.sort(Comparator.comparing(Event::getTick)
                .thenComparing(Event::getSequenceInTick)
                .thenComparing(Event::getId, Comparator.nullsLast(Long::compareTo)));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Event> listCityEvents(Long cityId, Long fromTick, Long toTick, Integer limit) {
        ensureCityExists(cityId);

        long effectiveFromTick = fromTick == null ? 0L : fromTick;
        if (effectiveFromTick < 0L) {
            throw new IllegalArgumentException("fromTick must be >= 0");
        }
        if (toTick != null && toTick < effectiveFromTick) {
            throw new IllegalArgumentException("toTick must be >= fromTick");
        }
        int effectiveLimit = normalizeLimit(limit);

        return eventRepository.findByCityIdOrderByTickAscSequenceInTickAscIdAsc(cityId).stream()
                .filter(event -> event.getTick() >= effectiveFromTick)
                .filter(event -> toTick == null || event.getTick() <= toTick)
                .limit(effectiveLimit)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Event> listCityEventsByType(Long cityId, EventType eventType) {
        ensureCityExists(cityId);
        return eventRepository.findByCityIdAndEventTypeOrderByTickAscSequenceInTickAscIdAsc(cityId, eventType);
    }

    private Comparator<EventDraft> eventDraftComparator() {
        return Comparator
                .comparing(EventDraft::actorIds, EventApplicationService::compareActorIdLists)
                .thenComparing(EventDraft::payloadDiscriminator)
                .thenComparing(draft -> draft.payload().toString())
                .thenComparing(draft -> draft.eventType().name());
    }

    private static int compareActorIdLists(List<Long> left, List<Long> right) {
        int minSize = Math.min(left.size(), right.size());
        for (int i = 0; i < minSize; i++) {
            int compared = Long.compare(left.get(i), right.get(i));
            if (compared != 0) {
                return compared;
            }
        }
        return Integer.compare(left.size(), right.size());
    }

    private int nextSequenceInCategory(Long cityId, long tick, EventCategory category) {
        int base = category.getPrecedence() * SEQUENCE_BUCKET_SIZE;
        return eventRepository.findTopByCityIdAndTickAndEventCategoryOrderBySequenceInTickDesc(cityId, tick, category)
                .map(Event::getSequenceInTick)
                .map(sequence -> sequence + 1)
                .orElse(base + 1);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_QUERY_LIMIT;
        }
        if (limit <= 0 || limit > MAX_QUERY_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_QUERY_LIMIT);
        }
        return limit;
    }

    private void ensureCityExists(Long cityId) {
        Objects.requireNonNull(cityId, "cityId must not be null");
        if (!cityRepository.existsById(cityId)) {
            throw new EntityNotFoundException("City not found with id: " + cityId);
        }
    }

    private String buildEventKey(EventType type, long tick, int sequenceInTick) {
        return type.name() + ":" + tick + ":" + sequenceInTick;
    }
}
