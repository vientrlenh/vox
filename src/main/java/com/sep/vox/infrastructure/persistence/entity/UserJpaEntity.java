package com.sep.vox.infrastructure.persistence.entity;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import com.sep.vox.infrastructure.shared.BaseEntity;

import jakarta.persistence.CheckConstraint;
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
@Table(name = "users", indexes = {
    @Index(columnList = "email", name = "idx_user_email", unique = true),
    @Index(columnList = "phone", name = "idx_user_phone", unique = true)
})
@Getter 
@Setter 
@NoArgsConstructor(access = AccessLevel.PROTECTED) 
@SuperBuilder 
public class UserJpaEntity extends BaseEntity {
    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "role", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_users_role_valid", 
            constraint = "role IN ('SYSTEM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER', 'STUDENT')"
        )
    })
    private String role;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "full_name", length = 255, nullable = false)
    private String fullName;

    @Column(name = "gender", length = 15)
    private String gender;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "avatar_url", length = 4096)
    private String avatarUrl;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_users_status_valid", 
            constraint = "status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'DISABLED')"
        )
    })
    private String status;

    @UpdateTimestamp 
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
