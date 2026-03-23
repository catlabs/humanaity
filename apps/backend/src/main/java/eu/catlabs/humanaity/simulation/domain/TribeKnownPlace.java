package eu.catlabs.humanaity.simulation.domain;

import eu.catlabs.humanaity.city.domain.City;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "tribe_known_place",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tribe_known_place_city_tribe_place", columnNames = {"city_id", "tribe_id", "place_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TribeKnownPlace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(name = "tribe_id", nullable = false, length = 64)
    private String tribeId;

    @Column(name = "place_id", nullable = false, length = 64)
    private String placeId;

    @Column(name = "discovered_by_human_id", nullable = false)
    private Long discoveredByHumanId;

    @Column(name = "discovered_tick", nullable = false)
    private Long discoveredTick;

    @Column(name = "reported_tick")
    private Long reportedTick;

    @Column(nullable = false)
    private boolean reported;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }
}
