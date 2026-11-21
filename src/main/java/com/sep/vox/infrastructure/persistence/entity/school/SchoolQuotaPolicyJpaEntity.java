package com.sep.vox.infrastructure.persistence.entity.school;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "school_quota_policies",
    // Restates the V5 constraint here because the test profile runs ddl-auto=create-drop: the schema the tests face is built
    // by Hibernate from the annotations, not by Flyway from the migrations.
    check = @CheckConstraint(
        name = "chk_school_quota_policies_ratio_in_range",
        constraint = "distributable_ratio >= 0 AND distributable_ratio <= 1"
    )
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolQuotaPolicyJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "quota_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_quota_policies_quota_type_valid",
            constraint = "quota_type IN ('ASSESSMENT', 'PRACTICE')"
        )
    })
    private String quotaType;

    // updatable = true, unlike most columns on the ledger tables: this is CONFIGURATION, editing in place is correct
    // business-wise -- it is not an entry that must stay unchanged.
    @Column(name = "distributable_ratio", nullable = false, precision = 5, scale = 4)
    private BigDecimal distributableRatio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
