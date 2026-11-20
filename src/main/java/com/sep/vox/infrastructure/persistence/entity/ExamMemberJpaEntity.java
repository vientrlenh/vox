package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// TODO(perf): thiếu index (exam_id, user_id) gây EXISTS-subquery chậm ở
// SpringDataQuestionRepository.findAccessibleByIdIn. Chờ được yêu cầu mới insert:
// @Table(name = "exam_members", indexes = {
//     @Index(columnList = "exam_id, user_id", name = "idx_exam_members_exam_user"),
//     @Index(columnList = "user_id", name = "idx_exam_members_user")
// })
@Entity
@Table(name = "exam_members")
public class ExamMemberJpaEntity {
    
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

    @Column(name = "exam_id", nullable = false, updatable = false)
    private UUID examId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "role", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "role", 
            constraint = "role IN ('CHAIR', 'AUTHOR', 'REVIEWER')"
        )
    })
    private String role;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "granted_by")
    private UUID grantedBy;

    protected ExamMemberJpaEntity() {}

    public ExamMemberJpaEntity(UUID id, UUID examId, UUID userId, String role, Instant grantedAt,
            UUID grantedBy) {
        this.id = id;
        this.examId = examId;
        this.userId = userId;
        this.role = role;
        this.grantedAt = grantedAt;
        this.grantedBy = grantedBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getExamId() {
        return examId;
    }

    public void setExamId(UUID examId) {
        this.examId = examId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }

    public UUID getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(UUID grantedBy) {
        this.grantedBy = grantedBy;
    }

    
}
