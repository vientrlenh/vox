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
@Table(name = "framework_result_bands", indexes = {
    @Index(columnList = "framework_version_id, code", name = "idx_framework_result_bands_version_code", unique = true),
    @Index(columnList = "framework_version_id, label", name = "idx_framework_result_bands_version_label", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FrameworkResultBandJpaEntity extends BaseEntity {

    @Column(name = "framework_version_id", nullable = false, updatable = false)
    private UUID frameworkVersionId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "result_band_order", nullable = false)
    private int order;
}
