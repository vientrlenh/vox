package com.sep.vox.infrastructure.persistence.entity.school;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.infrastructure.shared.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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
@Getter 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder 
public class SchoolUserJpaEntity extends BaseEntity {

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;
    
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "member_code", nullable = false, updatable = false)
    private String memberCode;

    @Column(name = "role", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_users_role_valid", 
            constraint = "role IN ('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')"
        )
    })
    private String role;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;
    
}
