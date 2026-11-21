package com.sep.vox.infrastructure.persistence.entity.user;

import java.time.LocalDate;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

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
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserJpaEntity extends BaseEntity {

    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "full_name", length = 255, nullable = false)
    private String fullName;

    @Column(name = "gender", length = 15)
    private String gender;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

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
}
