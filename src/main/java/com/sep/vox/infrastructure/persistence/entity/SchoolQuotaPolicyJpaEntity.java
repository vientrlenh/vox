package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "school_quota_policies",
    // Khai lại ràng buộc của V5 ở đây vì profile test chạy ddl-auto=create-drop: schema mà test đối
    // mặt do Hibernate dựng từ annotation, không phải do Flyway dựng từ migration.
    check = @CheckConstraint(
        name = "chk_school_quota_policies_ratio_in_range",
        constraint = "distributable_ratio >= 0 AND distributable_ratio <= 1"
    )
)
public class SchoolQuotaPolicyJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id",
        nullable = false,
        updatable = false,
        insertable = false,
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "quota_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_quota_policies_quota_type_valid",
            constraint = "quota_type IN ('EXAM', 'PRACTICE')"
        )
    })
    private String quotaType;

    // updatable = true, khác hầu hết cột ở các bảng sổ cái: đây là CẤU HÌNH, sửa tại chỗ là đúng
    // nghiệp vụ -- không phải một bút toán phải giữ nguyên.
    @Column(name = "distributable_ratio", nullable = false, precision = 5, scale = 4)
    private BigDecimal distributableRatio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SchoolQuotaPolicyJpaEntity() {}

    public SchoolQuotaPolicyJpaEntity(UUID id, UUID schoolId, String quotaType, BigDecimal distributableRatio,
            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.quotaType = quotaType;
        this.distributableRatio = distributableRatio;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public String getQuotaType() {
        return quotaType;
    }

    public BigDecimal getDistributableRatio() {
        return distributableRatio;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
