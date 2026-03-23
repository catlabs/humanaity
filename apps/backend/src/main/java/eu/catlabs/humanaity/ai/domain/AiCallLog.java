package eu.catlabs.humanaity.ai.domain;

import eu.catlabs.humanaity.city.domain.City;
import jakarta.persistence.Column;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "ai_call_log",
        indexes = {
                @Index(name = "idx_ai_call_log_requested_at", columnList = "requested_at,id"),
                @Index(name = "idx_ai_call_log_city_context", columnList = "city_id,context_type,success")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false, length = 40)
    private AiCallContextType contextType;

    @Column(name = "context_entity_type", length = 64)
    private String contextEntityType;

    @Column(name = "context_entity_id", length = 128)
    private String contextEntityId;

    @Column(length = 32)
    private String provider;

    @Column(length = 128)
    private String model;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "fallback_used", nullable = false)
    private boolean fallbackUsed;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "prompt_summary", length = 500)
    private String promptSummary;

    @Column(name = "response_summary", length = 500)
    private String responseSummary;

    @Column(name = "prompt_hash", length = 64)
    private String promptHash;

    @Column(name = "response_hash", length = 64)
    private String responseHash;

    @Column(name = "error_code", length = 128)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        requestedAt = requestedAt == null ? now : requestedAt;
        updatedAt = now;
        if (contextType == null) {
            contextType = AiCallContextType.UNSPECIFIED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
