package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "school_users", indexes = {
    @Index(columnList = "user_id", name = "idx_school_users_user", unique = true), 
    @Index(columnList = "school_id, user_id", name = "idx_school_users_school_user", unique = true)
}, check = {
    @CheckConstraint(
        name = "chk_school_users_start_date_and_end_date_valid", 
        constraint = "start_date < end_date"
    )
})
public class SchoolUserJpaEntity {
    
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
    
    @Column(name = "student_id", length = 255)
    private String studentId; 

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;
    
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private OffsetDateTime endDate;

    protected SchoolUserJpaEntity() {}

    public SchoolUserJpaEntity(UUID id, String studentId, UUID schoolId, UUID userId, OffsetDateTime startDate,
            OffsetDateTime endDate) {
        this.id = id;
        this.studentId = studentId;
        this.schoolId = schoolId;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(OffsetDateTime endDate) {
        this.endDate = endDate;
    }

    
}
