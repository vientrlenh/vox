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
@Table(name = "question_banks", indexes = {
    @Index(columnList = "owner_type, language_id, school_id, code", name = "idx_question_banks_language_school_code", unique = true)
}, check = {
    @CheckConstraint(
        name = "chk_owner_type_and_school_id_valid", 
        constraint = """
        (
            owner_type = 'SYSTEM' AND school_id IS NULL
        )   
        OR 
        (
            owner_type = 'SCHOOL' AND school_id IS NOT NULL
        )     
        """
    )
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionBankJpaEntity extends BaseEntity {

    @Column(name = "language_id", nullable = false, updatable = false)
    private UUID languageId;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "owner_type", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_question_banks_owner_type_valid", 
            constraint = "owner_type IN ('SYSTEM', 'SCHOOL')"
        )
    })
    private String ownerType;

    @Column(name = "status", nullable = false, check = {
        @CheckConstraint(
            name = "chk_quesion_banks_status_valid", 
            constraint = "status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')"
        )
    })
    private String status;
}
