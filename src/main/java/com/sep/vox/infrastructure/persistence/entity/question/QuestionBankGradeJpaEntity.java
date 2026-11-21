package com.sep.vox.infrastructure.persistence.entity.question;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

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
@Table(name = "question_bank_grades", indexes = {
    @Index(columnList = "question_bank_id, school_grade_id", name = "idx_question_bank_grades_bank_grade", unique = true),
    @Index(columnList = "school_grade_id", name = "idx_question_bank_grades_school_grade")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionBankGradeJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "question_bank_id", nullable = false, updatable = false)
    private UUID questionBankId;

    @Column(name = "school_grade_id", nullable = false, updatable = false)
    private UUID schoolGradeId;

    @Column(name = "attached_at", nullable = false, updatable = false)
    private Instant attachedAt;

    @Column(name = "attached_by", updatable = false)
    private UUID attachedBy;
}
