package com.sep.vox.infrastructure.persistence.entity.school;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UuidGenerator.Style;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "school_class_users", indexes = {
    @Index(columnList = "user_id, school_class_id", name = "idx_school_class_users", unique = true),
    @Index(columnList = "user_id", name = "idx_school_class_users_user_id")
})
@Getter 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder 
public class SchoolClassUserJpaEntity {
    
    @Id
    @GeneratedValue 
    @UuidGenerator(style = Style.VERSION_7)
    @Column(
        name = "id", 
        nullable = false, 
        updatable = false
    )
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "school_class_id", nullable = false, updatable = false)
    private UUID schoolClassId;

    @Column(name = "join_at", nullable = false, updatable = false)
    private Instant joinAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "assigned_by", nullable = false, updatable = false)
    private UUID assignedBy;

}
