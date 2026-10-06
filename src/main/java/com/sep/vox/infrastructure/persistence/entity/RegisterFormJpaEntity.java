package com.sep.vox.infrastructure.persistence.entity;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.infrastructure.common.BaseEntity;

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
@Table(name = "register_forms", indexes = {
        @Index(columnList = "identity_number", name = "idx_register_identity"),
        @Index(columnList = "contact_phone", name = "idx_register_phone"),
        @Index(columnList = "contact_email", name = "idx_register_email")
})
@Getter 
@Setter 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder 
public class RegisterFormJpaEntity extends BaseEntity {

    @Column(name = "school_directory_id", updatable = false)
    private UUID schoolDirectoryId;

    @Column(name = "school_name", length = 255)
    private String schoolName;

    @Column(name = "school_domain", updatable = false, length = 100)
    private String schoolDomain;

    @Column(name = "school_ward_id", updatable = false)
    private UUID schoolWardId;

    @Column(name = "school_city_id", updatable = false)
    private UUID schoolCityId;

    @Column(name = "school_province_id", updatable = false)
    private UUID schoolProvinceId; 

    @Column(name = "school_street_address",  length = 512)
    private String schoolStreetAddress;

    @Column(name = "contact_full_name", nullable = false, updatable = false, length = 255)
    private String contactFullName;

    @Column(name = "identity_number", nullable = false, updatable = false, length = 20)
    private String identityNumber;

    @Column(name = "contact_phone", nullable = false, updatable = false, length = 20)
    private String contactPhone;

    @Column(name = "contact_email", nullable = false, updatable = false, length = 255)
    private String contactEmail;

    @Column(name = "birth_date", nullable = false, updatable = false)
    private LocalDate birthDate;

    @Column(name = "contact_address", nullable = false, updatable = false, length = 512)
    private String contactAddress;

    @Column(name = "postal_code", nullable = false, updatable = false, length = 10)
    private String postalCode;

    @Column(name = "student_count", nullable = false, updatable = false)
    private Integer studentCount;

    @Column(name = "verification_method", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_register_form_verification_method_valid", 
            constraint = "verification_method IN ('DOMAIN_OTP', 'DOCUMENT')"
        )
    })
    private String verificationMethod;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "status", length = 20, nullable = false, check = {
        @CheckConstraint(
            name = "chk_register_forms_status_valid", 
            constraint = "status IN ('PENDING', 'AUTO_APPROVED', 'APPROVED', 'REJECTED')"
        )
    })
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    
}
