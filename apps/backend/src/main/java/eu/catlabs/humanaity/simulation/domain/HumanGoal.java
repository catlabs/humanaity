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
    @Column(name = "goal_type", nullable = false, length = 40)
    private HumanGoalType goalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private HumanGoalStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 40)
    private HumanGoalSource source;

    @Column(name = "assigned_tick", nullable = false)
    private Long assignedTick;

    @Column(name = "completed_tick")
    private Long completedTick;

    @Column(name = "target_place_id", length = 64)
    private String targetPlaceId;

    @Column(name = "target_human_id")
    private Long targetHumanId;

    @Column(name = "target_x")
    private Double targetX;

    @Column(name = "target_y")
    private Double targetY;

    @Column(name = "metadata_key", length = 64)
    private String metadataKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
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
