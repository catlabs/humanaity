package eu.catlabs.humanaity.event.application;

import eu.catlabs.humanaity.event.domain.EventCategory;
import eu.catlabs.humanaity.event.domain.EventType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record EventDraft(
        EventType eventType,
        List<Long> actorIds,
        Map<String, String> payload,
        int importance,
        String payloadDiscriminator
) {
    public EventDraft {
        Objects.requireNonNull(eventType, "eventType must not be null");
        actorIds = actorIds == null
                ? List.of()
                : actorIds.stream().filter(Objects::nonNull).sorted().toList();
        payload = payload == null
                ? Map.of()
                : Collections.unmodifiableMap(new TreeMap<>(payload));
        payloadDiscriminator = payloadDiscriminator == null ? "" : payloadDiscriminator;
    }

    public EventCategory eventCategory() {
        return eventType.getCategory();
    }
}
