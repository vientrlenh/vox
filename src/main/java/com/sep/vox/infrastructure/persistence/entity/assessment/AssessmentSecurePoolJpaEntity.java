package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assessment_secure_pools")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentSecurePoolJpaEntity extends BaseEntity {

    @Column(name = "assessment_id", nullable = false, updatable = false)
    private UUID assessmentId;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_secure_pools_status_valid",
            constraint = "status IN ('SEALED', 'RELEASED')"
        )
    })
    private String status;

    @Column(name = "release_mode", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_secure_pools_release_mode_valid",
            constraint = "release_mode IN ('MANUAL', 'AUTO_AFTER_CLOSE')"
        )
    })
    private String releaseMode;

    @Column(name = "embargo_until")
    private Instant embargoUntil;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "released_by")
    private UUID releasedBy;
}
