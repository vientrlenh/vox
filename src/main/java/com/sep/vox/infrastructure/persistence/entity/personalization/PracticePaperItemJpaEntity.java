package com.sep.vox.infrastructure.persistence.entity.personalization;

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
    name = "practice_paper_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_practice_paper_item_slot",
        columnNames = {"practice_paper_id", "slot_order"}
    ),
    indexes = @Index(
        name = "idx_practice_paper_item_paper",
        columnList = "practice_paper_id"
    )
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PracticePaperItemJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "practice_paper_id", nullable = false, updatable = false)
    private UUID practicePaperId;

    @Column(name = "practice_question_id", nullable = false, updatable = false)
    private UUID practiceQuestionId;

    @Column(name = "slot_order", nullable = false, updatable = false)
    private int slotOrder;

    @Column(name = "target_criterion_code", nullable = false, length = 32, updatable = false)
    private String targetCriterionCode;

    @Column(name = "target_sub_attribute", length = 64, updatable = false)
    private String targetSubAttribute;

    @Column(name = "target_difficulty_rank", nullable = false, updatable = false)
    private int targetDifficultyRank;
}
