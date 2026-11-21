package com.sep.vox.infrastructure.persistence.entity.assessment;

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
@Table(name = "assessment_papers")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentPaperJpaEntity extends BaseEntity {

    @Column(name = "assessment_id", nullable = false, updatable = false)
    private UUID assessmentId;

    @Column(name = "blueprint_version_id")
    private UUID blueprintVersionId;

    @Column(name = "code", nullable = false, updatable = false, length = 100)
    private String code;

    @Column(name = "variant", nullable = false)
    private int variant;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_papers_status_valid",
            constraint = "status IN ('DRAFT', 'IN_REVIEW', 'APPROVED', 'LOCKED')"
        )
    })
    private String status;

    @Column(name = "time_duration_seconds")
    private Integer timeDurationSeconds;

}
