package com.sep.vox.infrastructure.persistence.entity.framework;

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
@Table(name = "frameworks", indexes = {
    @Index(columnList = "code", name = "idx_frameworks_code", unique = true),
    @Index(columnList = "is_active", name = "idx_frameworks_active")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FrameworkJpaEntity extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50, updatable = false)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
