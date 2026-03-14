package eu.catlabs.humanaity.simulation.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "director_intervention")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirectorIntervention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long cityId;

    @Column(nullable = false)
    private Long initiatedByUserId;

    @Column(nullable = false, length = 64)
    private String commandType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DirectorInterventionStatus status;

    @Column(nullable = false, length = 64, unique = true)
    private String confirmationToken;

    @Column(nullable = false)
    private Instant confirmationExpiresAt;

    @ElementCollection
    @CollectionTable(name = "director_intervention_actor_ids", joinColumns = @JoinColumn(name = "intervention_id"))
    @Column(name = "actor_id", nullable = false)
    @OrderColumn(name = "actor_index")
    private List<Long> actorHumanIds = new ArrayList<>();

    @Column(length = 512)
    private String summary;

    private Long requestedAtTick;
    private Long executedAtTick;
    private Instant confirmedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (actorHumanIds == null) {
            actorHumanIds = new ArrayList<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
