package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;
import com.sep.vox.infrastructure.shared.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "grade_levels", indexes = {
    @Index(columnList = "code", name = "idx_grade_levels_code", unique = true),
    @Index(columnList = "grade_level_order", name = "idx_grade_levels_order", unique = true)
})
@Getter 
@NoArgsConstructor(access = AccessLevel.PROTECTED) 
@SuperBuilder 
public class GradeLevelJpaEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "grade_level_order", nullable = false, check = {
        @CheckConstraint(
            name = "chk_grade_levels_order_positive",
            constraint = "grade_level_order > 0"
        )
    })
    private Integer order;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_grade_levels_status_valid",
            constraint = "status IN ('ACTIVE', 'INACTIVE')"
        )
    })
    private String status;

    @UpdateTimestamp 
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

}
