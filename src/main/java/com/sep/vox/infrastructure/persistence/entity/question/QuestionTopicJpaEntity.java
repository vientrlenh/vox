package com.sep.vox.infrastructure.persistence.entity.question;

import java.util.UUID;

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
@Table(name = "question_topics", indexes = {
    @Index(columnList = "question_bank_id, code", name = "idx_question_topics_question_bank_code", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionTopicJpaEntity extends BaseEntity {

    @Column(name = "question_bank_id", nullable = false, updatable = false)
    private UUID questionBankId;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_question_topics_status_valid", 
            constraint = "status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')"
        )
    })
    private String status;
}
