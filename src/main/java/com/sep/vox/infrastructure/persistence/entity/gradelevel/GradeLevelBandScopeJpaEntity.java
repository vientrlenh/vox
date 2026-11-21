package com.sep.vox.infrastructure.persistence.entity.gradelevel;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * See V42__grade_level_band_scopes.sql. The ceiling is stored as a band ID, not as result_band_order,
 * so there is no "default <= hard_max" check constraint here: comparing the order needs a join to
 * framework_result_bands, which GradeLevelBandScopeGuardService does.
 */
@Entity
@Table(name = "grade_level_band_scopes", indexes = {
    @Index(columnList = "grade_level_id, framework_version_id", name = "idx_grade_level_band_scopes_level_framework", unique = true),
    @Index(columnList = "default_target_band_id", name = "idx_grade_level_band_scopes_default_band"),
    @Index(columnList = "hard_max_band_id", name = "idx_grade_level_band_scopes_hard_max_band")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GradeLevelBandScopeJpaEntity extends BaseEntity {

    @Column(name = "grade_level_id", nullable = false, updatable = false)
    private UUID gradeLevelId;

    @Column(name = "framework_version_id", nullable = false, updatable = false)
    private UUID frameworkVersionId;

    @Column(name = "default_target_band_id", nullable = false)
    private UUID defaultTargetBandId;

    @Column(name = "hard_max_band_id", nullable = false)
    private UUID hardMaxBandId;
}
