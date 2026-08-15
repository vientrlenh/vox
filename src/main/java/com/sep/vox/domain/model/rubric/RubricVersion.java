package com.sep.vox.domain.model.rubric;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;



public class RubricVersion {
    private UUID id;
    private UUID rubricId;
    private int version;
    private String code;
    private String name;
    private String description;
    private RubricStatus status;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private BigDecimal scoringScaleMin;
    private BigDecimal scoringScaleMax;
    private RubricTotalScoreMethod totalScoreMethod;
    /**
     * Phiên bản gốc mà bản này được sao ra, hoặc null nếu do chính chủ sở hữu soạn.
     *
     * <p>Cố ý KHÔNG nằm trong constructor: thêm tham số vào đó buộc mọi nơi đang dựng RubricVersion
     * phải sửa theo, tuyệt đại đa số chỉ để truyền null. Bên sao chép tự gọi setter.
     */
    private UUID sourceRubricVersionId;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public RubricVersion() {}

    public RubricVersion(UUID id, UUID rubricId, int version, String code, String name, String description,
            RubricStatus status, Instant effectiveFrom, Instant effectiveTo, BigDecimal scoringScaleMin,
            BigDecimal scoringScaleMax, RubricTotalScoreMethod totalScoreMethod, Instant createdAt,
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.rubricId = rubricId;
        this.version = version;
        this.code = code;
        this.name = name;
        this.description = description;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.scoringScaleMin = scoringScaleMin;
        this.scoringScaleMax = scoringScaleMax;
        this.totalScoreMethod = totalScoreMethod;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public RubricVersion(UUID rubricId, int version, String code, String name, String description, RubricStatus status,
            Instant effectiveFrom, Instant effectiveTo, BigDecimal scoringScaleMin,
            BigDecimal scoringScaleMax, RubricTotalScoreMethod totalScoreMethod, Instant createdAt,
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.rubricId = rubricId;
        this.version = version;
        this.code = code;
        this.name = name;
        this.description = description;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.scoringScaleMin = scoringScaleMin;
        this.scoringScaleMax = scoringScaleMax;
        this.totalScoreMethod = totalScoreMethod;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRubricId() {
        return rubricId;
    }

    public void setRubricId(UUID rubricId) {
        this.rubricId = rubricId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RubricStatus getStatus() {
        return status;
    }

    public void setStatus(RubricStatus status) {
        this.status = status;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public BigDecimal getScoringScaleMin() {
        return scoringScaleMin;
    }

    public void setScoringScaleMin(BigDecimal scoringScaleMin) {
        this.scoringScaleMin = scoringScaleMin;
    }

    public BigDecimal getScoringScaleMax() {
        return scoringScaleMax;
    }

    public void setScoringScaleMax(BigDecimal scoringScaleMax) {
        this.scoringScaleMax = scoringScaleMax;
    }

    public RubricTotalScoreMethod getTotalScoreMethod() {
        return totalScoreMethod;
    }

    public void setTotalScoreMethod(RubricTotalScoreMethod totalScoreMethod) {
        this.totalScoreMethod = totalScoreMethod;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public UUID getSourceRubricVersionId() {
        return sourceRubricVersionId;
    }

    public void setSourceRubricVersionId(UUID sourceRubricVersionId) {
        this.sourceRubricVersionId = sourceRubricVersionId;
    }

    
    
}
