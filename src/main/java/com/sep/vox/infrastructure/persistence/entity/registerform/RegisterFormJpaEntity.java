package com.sep.vox.infrastructure.persistence.entity.registerform;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "register_forms", indexes = {
        @Index(columnList = "identity_number", name = "idx_register_identity"),
        @Index(columnList = "contact_phone", name = "idx_register_phone"),
        @Index(columnList = "contact_email", name = "idx_register_email")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegisterFormJpaEntity {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "school_directory_id", updatable = false)
    private UUID schoolDirectoryId;

    @Column(name = "school_name", length = 255)
    private String schoolName;

    @Column(name = "school_domain", updatable = false, length = 100)
    private String schoolDomain;

    @Column(name = "school_district", length = 255)
    private String schoolDistrict;

    @Column(name = "school_province", length = 255)
    private String schoolProvince; 

    @Column(name = "school_address",  length = 512)
    private String schoolAddress;

    @Column(name = "contact_full_name", nullable = false, updatable = false, length = 255)
    private String contactFullName;

    @Column(name = "identity_number", nullable = false, updatable = false, length = 20)
    private String identityNumber;

    @Column(name = "contact_phone", nullable = false, updatable = false, length = 20)
    private String contactPhone;

    @Column(name = "contact_email", nullable = false, updatable = false, length = 255)
    private String contactEmail;

    @Column(name = "date_of_birth", nullable = false, updatable = false)
    private LocalDate dateOfBirth;

    @Column(name = "contact_address", nullable = false, updatable = false, length = 512)
    private String contactAddress;

    @Column(name = "postal_code", nullable = false, updatable = false, length = 10)
    private String postalCode;

    @Column(name = "position", nullable = false, updatable = false, length = 50)
    private String position;

    @Column(name = "student_count", nullable = false, updatable = false)
    private int studentCount;

    @Column(name = "verification_method", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_register_form_verification_method_valid", 
            constraint = "verification_method IN ('DOMAIN_OTP', 'DOCUMENT')"
        )
    })
    private String verificationMethod;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "rejected_reason", length = 255)
    private String rejectedReason;

    @Column(name = "status", length = 20, nullable = false, check = {
        @CheckConstraint(
            name = "chk_register_forms_status_valid", 
            constraint = "status IN ('PENDING', 'AUTO_APPROVED', 'APPROVED', 'REJECTED')"
        )
    })
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;
}
