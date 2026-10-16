package com.sep.vox.infrastructure.persistence.entity.school;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.infrastructure.shared.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.CheckConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "school_rooms", indexes = {
    @Index(columnList = "school_id, code", name = "school_rooms_school_code", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder 
public class SchoolRoomJpaEntity extends BaseEntity {
    
    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_rooms_status_valid", 
            constraint = "status IN ('INACTIVE', 'ACTIVE', 'ARCHIVED')"
        )
    })
    private String status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

}
