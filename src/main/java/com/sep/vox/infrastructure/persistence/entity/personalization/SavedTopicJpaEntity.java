package com.sep.vox.infrastructure.persistence.entity.personalization;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "saved_topics",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_saved_topic_student_topic",
        columnNames = {"student_id", "practice_topic_id"}
    ),
    indexes = @Index(name = "idx_saved_topic_student", columnList = "student_id")
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedTopicJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "practice_topic_id", nullable = false, updatable = false)
    private UUID practiceTopicId;

    @Column(name = "saved_at", nullable = false, updatable = false)
    private Instant savedAt;
}
