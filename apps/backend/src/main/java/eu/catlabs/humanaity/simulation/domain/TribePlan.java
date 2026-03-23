package eu.catlabs.humanaity.simulation.domain;

import eu.catlabs.humanaity.city.domain.City;
import eu.catlabs.humanaity.simulation.application.tribe.TribeDecisionType;
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
        name = "simulation_tribe_plan",
        indexes = {
                @Index(name = "idx_simulation_tribe_plan_city_status", columnList = "city_id,plan_status,tribe_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_simulation_tribe_plan_city_tribe", columnNames = {"city_id", "tribe_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TribePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(name = "tribe_id", nullable = false, length = 64)
    private String tribeId;

    @Column(name = "chief_human_id")
    private Long chiefHumanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", length = 32)
    private TribeDecisionType planType;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_status", nullable = false, length = 24)
    private TribePlanStatus planStatus;

    @Column(name = "target_place_id", length = 64)
    private String targetPlaceId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "simulation_tribe_plan_assigned_humans", joinColumns = @JoinColumn(name = "tribe_plan_id"))
    @Column(name = "human_id", nullable = false)
    @OrderColumn(name = "assignment_index")
    private List<Long> assignedHumanIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_source", length = 24)
    private TribeDecisionSource decisionSource;

    @Column(name = "reason_summary", length = 500)
    private String reasonSummary;

    @Column(name = "plan_metadata_key", length = 128)
    private String planMetadataKey;

    @Column(name = "last_assigned_tick")
    private Long lastAssignedTick;

    @Column(name = "completed_tick")
    private Long completedTick;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (assignedHumanIds == null) {
            assignedHumanIds = new ArrayList<>();
        }
        if (planStatus == null) {
            planStatus = TribePlanStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        if (assignedHumanIds == null) {
            assignedHumanIds = new ArrayList<>();
        }
    }
}
