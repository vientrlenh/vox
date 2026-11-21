package com.sep.vox.infrastructure.persistence.entity.question;

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
@Table(name = "question_collaborators", indexes = {
    @Index(columnList = "question_id, user_id", name = "idx_question_collaborators_question_user", unique = true),
    @Index(columnList = "user_id", name = "idx_question_collaborators_user")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionCollaboratorJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    @Column(name = "permission", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_question_collaborators_permission_valid",
            constraint = "permission IN ('READ_ONLY', 'CAN_USE', 'CAN_EDIT')"
        )
    })
    private String permission;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;
}
