package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "interest_dimension")
public class InterestDimensionJpaEntity {

    @Id
    @Column(name = "code", nullable = false, updatable = false, length = 32)
    private String code;

    @Column(name = "label", nullable = false, length = 128)
    private String label;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "quiz_eligible", nullable = false)
    private boolean quizEligible = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected InterestDimensionJpaEntity() {
    }

    public InterestDimensionJpaEntity(
            String code,
            String label,
            String description,
            boolean active,
            boolean quizEligible,
            int displayOrder,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        this.code = code;
        this.label = label;
        this.description = description;
        this.active = active;
        this.quizEligible = quizEligible;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isQuizEligible() {
        return quizEligible;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
