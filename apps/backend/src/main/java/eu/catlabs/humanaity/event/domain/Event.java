package eu.catlabs.humanaity.event.domain;

import eu.catlabs.humanaity.ai.domain.AiEnrichmentStatus;
import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.history.domain.HistoryEra;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(
        name = "history_event",
        indexes = {
                @Index(name = "idx_history_event_city_tick_seq", columnList = "city_id,tick,sequenceInTick"),
                @Index(name = "idx_history_event_city_type", columnList = "city_id,eventType")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_history_event_city_key", columnNames = {"city_id", "eventKey"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(nullable = false)
    private Long tick;

    @Column(nullable = false)
    private Integer sequenceInTick;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventCategory eventCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private EventType eventType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "history_event_actor_ids", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "actor_id", nullable = false)
    @OrderColumn(name = "actor_index")
    private List<Long> actorIds = new ArrayList<>();

    @Convert(converter = EventPayloadConverter.class)
    @Column(nullable = false, length = 4000)
    private Map<String, String> payload;

    @Column(nullable = false)
    private Integer importance;

    @Column(name = "event_year", nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private HistoryEra era;

    @Column(nullable = false, length = 128)
    private String eventKey;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AiEnrichmentStatus enrichmentStatus;

    @Column(nullable = false)
    private Boolean enrichmentFallback;

    @Column(length = 2000)
    private String enrichedSnippet;

    @Column(length = 64)
    private String enrichmentProvider;

    @Column(length = 128)
    private String enrichmentModel;

    private Instant enrichmentUpdatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (actorIds == null) {
            actorIds = new ArrayList<>();
        }
        if (payload == null) {
            payload = Map.of();
        }
        if (enrichmentStatus == null) {
            enrichmentStatus = AiEnrichmentStatus.NONE;
        }
        if (enrichmentFallback == null) {
            enrichmentFallback = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
