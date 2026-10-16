package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.infrastructure.shared.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Xem V42__grade_level_band_scopes.sql. Trần lưu bằng ID bậc chứ không bằng result_band_order --
 * nên ở đây không còn check constraint "default <= hard_max": so sánh thứ tự phải join sang
 * framework_result_bands, việc đó do GradeLevelBandScopeGuardService làm.
 */
@Entity
@Table(name = "grade_level_band_scopes", indexes = {
    @Index(columnList = "grade_level_id, framework_version_id", name = "idx_grade_level_band_scopes_level_framework", unique = true),
    @Index(columnList = "default_target_band_id", name = "idx_grade_level_band_scopes_default_band"),
    @Index(columnList = "hard_max_band_id", name = "idx_grade_level_band_scopes_hard_max_band")
})
@Getter 
@NoArgsConstructor(access = AccessLevel.PROTECTED) 
@SuperBuilder 
public class GradeLevelBandScopeJpaEntity extends BaseEntity {

    @Column(name = "grade_level_id", nullable = false, updatable = false)
    private UUID gradeLevelId;

    @Column(name = "framework_version_id", nullable = false, updatable = false)
    private UUID frameworkVersionId;

    @Column(name = "default_target_band_id", nullable = false)
    private UUID defaultTargetBandId;

    @Column(name = "hard_max_band_id", nullable = false)
    private UUID hardMaxBandId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

}
