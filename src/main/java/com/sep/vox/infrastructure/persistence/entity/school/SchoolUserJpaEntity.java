package com.sep.vox.infrastructure.persistence.entity.school;

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
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolUserJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;
}
