package eu.catlabs.humanaity.invention.domain;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.history.domain.HistoryEra;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
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

@Entity
@Table(
        name = "history_invention",
        indexes = {
                @Index(name = "idx_history_invention_city_tick", columnList = "city_id,tickCreated"),
                @Index(name = "idx_history_invention_city_category", columnList = "city_id,category")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_history_invention_city_key", columnNames = {"city_id", "inventionKey"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invention {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(nullable = false)
    private Long tickCreated;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InventionCategory category;

    @Column(nullable = false, length = 128)
    private String inventionKey;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String summary;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "history_invention_source_event_keys", joinColumns = @JoinColumn(name = "invention_id"))
    @Column(name = "source_event_key", nullable = false, length = 128)
    @OrderColumn(name = "source_index")
    private List<String> sourceEventKeys = new ArrayList<>();

    @Column(nullable = false)
    private Integer impactScore;

    @Column(nullable = false)
    private Integer yearCreated;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private HistoryEra eraCreated;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (sourceEventKeys == null) {
            sourceEventKeys = new ArrayList<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
