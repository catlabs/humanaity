package eu.catlabs.humanaity.invention.application;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.city.infrastructure.persistence.CityRepository;
import eu.catlabs.humanaity.event.domain.Event;
import eu.catlabs.humanaity.event.domain.EventType;
import eu.catlabs.humanaity.event.infrastructure.persistence.EventRepository;
import eu.catlabs.humanaity.history.domain.HistoryTimelineMapper;
import eu.catlabs.humanaity.invention.domain.Invention;
import eu.catlabs.humanaity.invention.domain.InventionCategory;
import eu.catlabs.humanaity.invention.infrastructure.persistence.InventionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class InventionApplicationService {

    private static final int MAX_QUERY_LIMIT = 1_000;
    private static final int DEFAULT_QUERY_LIMIT = 200;

    private final InventionRepository inventionRepository;
    private final EventRepository eventRepository;
    private final CityRepository cityRepository;

    public InventionApplicationService(
            InventionRepository inventionRepository,
            EventRepository eventRepository,
            CityRepository cityRepository
    ) {
        this.inventionRepository = inventionRepository;
        this.eventRepository = eventRepository;
        this.cityRepository = cityRepository;
    }

    @Transactional
    public List<Invention> deriveFromPersistedEvents(Long cityId) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + cityId));

        List<Event> discoveries = eventRepository.findByCityIdAndEventTypeOrderByTickAscSequenceInTickAscIdAsc(
                cityId,
                EventType.DISCOVERY_UNLOCKED
        );
        if (discoveries.isEmpty()) {
            return List.of();
        }

        Set<String> existingKeys = new HashSet<>();
        inventionRepository.findByCityIdOrderByTickCreatedAscInventionKeyAscIdAsc(cityId).stream()
                .map(Invention::getInventionKey)
                .forEach(existingKeys::add);

        List<Invention> created = new ArrayList<>();
        for (Event discovery : discoveries) {
            Map<String, String> payload = discovery.getPayload();
            String inventionKey = payload.get("inventionKey");
            if (isBlank(inventionKey) || existingKeys.contains(inventionKey)) {
                continue;
            }

            InventionCategory category = parseCategory(payload.get("inventionCategory"));
            String title = payload.get("title");
            String summary = payload.get("summary");
            Integer impactScore = parseImpactScore(payload.get("impactScore"), discovery.getImportance());

            if (category == null || isBlank(title) || isBlank(summary) || impactScore == null) {
                continue;
            }

            Invention invention = new Invention();
            invention.setCity(city);
            invention.setTickCreated(discovery.getTick());
            invention.setCategory(category);
            invention.setInventionKey(inventionKey);
            invention.setTitle(title);
            invention.setSummary(summary);
            invention.setSourceEventKeys(List.of(discovery.getEventKey()));
            invention.setImpactScore(impactScore);
            invention.setYearCreated(HistoryTimelineMapper.yearForTick(discovery.getTick()));
            invention.setEraCreated(HistoryTimelineMapper.eraForTick(discovery.getTick()));

            Invention saved = inventionRepository.save(invention);
            created.add(saved);
            existingKeys.add(inventionKey);
        }

        return created;
    }

    @Transactional(readOnly = true)
    public List<Invention> listCityInventions(Long cityId, Long fromTick, Long toTick, Integer limit) {
        ensureCityExists(cityId);

        long effectiveFromTick = fromTick == null ? 0L : fromTick;
        if (effectiveFromTick < 0L) {
            throw new IllegalArgumentException("fromTick must be >= 0");
        }
        if (toTick != null && toTick < effectiveFromTick) {
            throw new IllegalArgumentException("toTick must be >= fromTick");
        }
        int effectiveLimit = normalizeLimit(limit);

        return inventionRepository.findByCityIdOrderByTickCreatedAscInventionKeyAscIdAsc(cityId).stream()
                .filter(invention -> invention.getTickCreated() >= effectiveFromTick)
                .filter(invention -> toTick == null || invention.getTickCreated() <= toTick)
                .limit(effectiveLimit)
                .toList();
    }

    private void ensureCityExists(Long cityId) {
        Objects.requireNonNull(cityId, "cityId must not be null");
        if (!cityRepository.existsById(cityId)) {
            throw new EntityNotFoundException("City not found with id: " + cityId);
        }
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

    private InventionCategory parseCategory(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return InventionCategory.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Integer parseImpactScore(String value, Integer fallback) {
        if (isBlank(value)) {
            return fallback == null ? null : Math.max(0, fallback);
        }
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            return fallback == null ? null : Math.max(0, fallback);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
