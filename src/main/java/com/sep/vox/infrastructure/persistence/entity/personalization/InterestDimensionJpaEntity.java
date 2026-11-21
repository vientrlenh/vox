package com.sep.vox.infrastructure.persistence.entity.personalization;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "interest_dimensions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestDimensionJpaEntity {

    @Id
    @Column(name = "code", nullable = false, updatable = false, length = 32)
    private String code;

    @Column(name = "label", nullable = false, length = 128)
    private String label;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "quiz_eligible", nullable = false)
    @Builder.Default
    private boolean quizEligible = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
