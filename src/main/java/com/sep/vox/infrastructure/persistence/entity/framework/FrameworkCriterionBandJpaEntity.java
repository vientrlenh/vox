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
@Table(name = "framework_criterion_bands", indexes = {
    @Index(columnList = "framework_criterion_id, framework_result_band_id", name = "idx_framework_criterion_bands_criterion_result", unique = true),
    @Index(columnList = "framework_result_band_id", name = "idx_framework_criterion_bands_result")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FrameworkCriterionBandJpaEntity extends BaseEntity {

    @Column(name = "framework_criterion_id", nullable = false, updatable = false)
    private UUID frameworkCriterionId;

    @Column(name = "framework_result_band_id", nullable = false, updatable = false)
    private UUID frameworkResultBandId;

    @Column(name = "descriptor", columnDefinition = "TEXT")
    private String descriptor;

    @Column(name = "positive_signals_json", columnDefinition = "TEXT")
    private String positiveSignalsJson;

    @Column(name = "negative_signals_json", columnDefinition = "TEXT")
    private String negativeSignalsJson;
}
