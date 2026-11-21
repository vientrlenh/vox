package com.sep.vox.infrastructure.persistence.entity.gradelevel;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// Grade level catalog shared by the WHOLE SYSTEM (previously each school declared its own set in
// school_grade_levels), so uniqueness is now only on code/order, no longer together with school_id.
@Entity
@Table(name = "grade_levels", indexes = {
    @Index(columnList = "code", name = "idx_grade_levels_code", unique = true),
    @Index(columnList = "grade_level_order", name = "idx_grade_levels_order", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GradeLevelJpaEntity extends BaseEntity {

    @Column(name = "code", nullable = false, updatable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    // "order" is an SQL keyword, so the column keeps the name grade_level_order from the old table.
    @Column(name = "grade_level_order", nullable = false, check = {
        @CheckConstraint(
            name = "chk_grade_levels_order_positive",
            constraint = "grade_level_order > 0"
        )
    })
    private int order;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_grade_levels_status_valid",
            constraint = "status IN ('ACTIVE', 'INACTIVE')"
        )
    })
    private String status;
}
