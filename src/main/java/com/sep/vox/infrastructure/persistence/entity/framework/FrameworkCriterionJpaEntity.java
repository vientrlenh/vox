package com.sep.vox.infrastructure.persistence.entity.framework;

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

@Entity
@Table(name = "framework_criteria", indexes = {
    @Index(columnList = "framework_version_id, code", name = "idx_framework_criteria_version_code", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FrameworkCriterionJpaEntity extends BaseEntity {

    @Column(name = "framework_version_id", nullable = false, updatable = false)
    private UUID frameworkVersionId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "criteria_order", nullable = false)
    private int order;
}
