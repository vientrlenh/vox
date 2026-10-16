package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// TODO(perf): thiếu index (assessment_id, user_id) gây EXISTS-subquery chậm ở
// SpringDataQuestionRepository.findAccessibleByIdIn. Chờ được yêu cầu mới insert:
// @Table(name = "assessment_members", indexes = {
//     @Index(columnList = "assessment_id, user_id", name = "idx_assessment_members_assessment_user"),
//     @Index(columnList = "user_id", name = "idx_assessment_members_user")
// })
@Entity
@Table(name = "assessment_members")
public class AssessmentMemberJpaEntity {
    
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

    @Column(name = "assessment_id", nullable = false, updatable = false)
    private UUID assessmentId;

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

    protected AssessmentMemberJpaEntity() {}

    public AssessmentMemberJpaEntity(UUID id, UUID assessmentId, UUID userId, String role, Instant grantedAt,
            UUID grantedBy) {
        this.id = id;
        this.assessmentId = assessmentId;
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

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(UUID assessmentId) {
        this.assessmentId = assessmentId;
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
