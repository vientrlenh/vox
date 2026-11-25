package com.sep.vox.infrastructure.persistence.entity.question;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.UuidGenerator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "student_question_exposures",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_student_question_exposure_student_question",
        columnNames = {"student_id", "practice_question_id"}
    ),
    indexes = @Index(
        name = "idx_student_question_exposure_student_seen",
        columnList = "student_id, seen_at"
    )
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentQuestionExposureJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "practice_question_id", nullable = false)
    private UUID practiceQuestionId;

    @Column(name = "seen_at", nullable = false)
    private Instant seenAt;
}
