package eu.catlabs.humanaity.simulation.domain;

import eu.catlabs.humanaity.city.domain.City;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "simulation_knowledge_unlock",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_knowledge_unlock_city_node", columnNames = {"city_id", "node_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeUnlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(name = "node_id", nullable = false, length = 128)
    private String nodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 24)
    private TechTreeNodeType nodeType;

    @Column(name = "unlocked_tick", nullable = false)
    private Long unlockedTick;

    @Column(name = "trigger_event_type", length = 64)
    private String triggerEventType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
