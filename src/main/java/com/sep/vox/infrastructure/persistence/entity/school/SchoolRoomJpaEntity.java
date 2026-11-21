package com.sep.vox.infrastructure.persistence.entity.school;

import java.util.UUID;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "school_rooms", indexes = {
    @Index(columnList = "school_id, code", name = "school_rooms_school_code", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolRoomJpaEntity extends BaseEntity {

    
    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    
    @Column(name = "code", nullable = false, updatable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
