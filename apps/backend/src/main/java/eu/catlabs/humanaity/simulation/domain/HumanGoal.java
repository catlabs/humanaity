package eu.catlabs.humanaity.simulation.domain;

import eu.catlabs.humanaity.human.domain.Human;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "simulation_goal")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HumanGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "human_id", nullable = false)
    private Human human;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private HumanGoalType goalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private HumanGoalStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private HumanGoalSource source;

    @Column(nullable = false)
    private Long assignedTick;

    @Column
    private Long completedTick;

    @Column(length = 64)
    private String targetPlaceId;

    @Column
    private Long targetHumanId;

    @Column
    private Double targetX;

    @Column
    private Double targetY;

    @Column(length = 64)
    private String metadataKey;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = HumanGoalStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
