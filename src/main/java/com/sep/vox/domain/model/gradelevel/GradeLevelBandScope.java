package com.sep.vox.domain.model.gradelevel;

import java.time.Instant;
import java.util.UUID;

/**
 * Trần bậc mục tiêu cho một khối lớp trên một phiên bản khung năng lực.
 *
 * <p>Giữ ID bậc chứ không giữ thứ tự bậc: trần là một BẬC cụ thể ("không vượt quá B1"), không
 * phải một vị trí trên thang ("không vượt quá bậc số 3"). Nếu quản trị chèn thêm bậc hoặc đánh
 * lại thứ tự trong khung thì cách lưu theo ID vẫn giữ nguyên ý nghĩa, còn lưu theo thứ tự thì
 * trần âm thầm trỏ sang bậc khác.
 *
 * <p>Bất biến "bậc mặc định không được cao hơn bậc trần" và "cả hai bậc phải thuộc đúng
 * frameworkVersionId" không diễn đạt được ở tầng DB (đều cần join sang framework_result_bands),
 * nên do GradeLevelBandScopeGuardService canh.
 */
public class GradeLevelBandScope {
    private UUID id;
    private UUID gradeLevelId;
    private UUID frameworkVersionId;
    private UUID defaultTargetBandId;
    private UUID hardMaxBandId;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public GradeLevelBandScope() {}

    public GradeLevelBandScope(UUID id, UUID gradeLevelId, UUID frameworkVersionId, UUID defaultTargetBandId,
            UUID hardMaxBandId, Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.gradeLevelId = gradeLevelId;
        this.frameworkVersionId = frameworkVersionId;
        this.defaultTargetBandId = defaultTargetBandId;
        this.hardMaxBandId = hardMaxBandId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public GradeLevelBandScope(UUID gradeLevelId, UUID frameworkVersionId, UUID defaultTargetBandId,
            UUID hardMaxBandId, Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.gradeLevelId = gradeLevelId;
        this.frameworkVersionId = frameworkVersionId;
        this.defaultTargetBandId = defaultTargetBandId;
        this.hardMaxBandId = hardMaxBandId;
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

    public UUID getGradeLevelId() {
        return gradeLevelId;
    }

    public void setGradeLevelId(UUID gradeLevelId) {
        this.gradeLevelId = gradeLevelId;
    }

    public UUID getFrameworkVersionId() {
        return frameworkVersionId;
    }

    public void setFrameworkVersionId(UUID frameworkVersionId) {
        this.frameworkVersionId = frameworkVersionId;
    }

    public UUID getDefaultTargetBandId() {
        return defaultTargetBandId;
    }

    public void setDefaultTargetBandId(UUID defaultTargetBandId) {
        this.defaultTargetBandId = defaultTargetBandId;
    }

    public UUID getHardMaxBandId() {
        return hardMaxBandId;
    }

    public void setHardMaxBandId(UUID hardMaxBandId) {
        this.hardMaxBandId = hardMaxBandId;
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
}
