package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "school_users", indexes = {
    @Index(columnList = "user_id", name = "idx_school_users_user_id", unique = true),
    @Index(columnList = "school_id", name = "idx_school_users_school_id")
})
public class SchoolUserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(
        name = "school_user_seq_gen",
        sequenceName = "school_user_seq",
        allocationSize = 50
    )
    private long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "student_id", length = 100)
    private String studentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    protected SchoolUserJpaEntity() {}

    public SchoolUserJpaEntity(long id, UUID userId, UUID schoolId, String studentId, OffsetDateTime createdAt, UUID createdBy) {
        this.id = id;
        this.userId = userId;
        this.schoolId = schoolId;
        this.studentId = studentId;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public SchoolUserJpaEntity(UUID userId, UUID schoolId, String studentId, OffsetDateTime createdAt, UUID createdBy) {
        this.userId = userId;
        this.schoolId = schoolId;
        this.studentId = studentId;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}
