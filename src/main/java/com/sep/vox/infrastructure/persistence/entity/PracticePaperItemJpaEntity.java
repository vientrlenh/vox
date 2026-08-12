package com.sep.vox.infrastructure.persistence.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "practice_paper_item",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_practice_paper_item_slot",
        columnNames = {"practice_paper_id", "slot_order"}
    ),
    indexes = @Index(
        name = "idx_practice_paper_item_paper",
        columnList = "practice_paper_id"
    )
)
public class PracticePaperItemJpaEntity {

    @Id
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

    protected PracticePaperItemJpaEntity() {
    }

    public PracticePaperItemJpaEntity(
            UUID id,
            UUID practicePaperId,
            UUID practiceQuestionId,
            int slotOrder,
            String targetCriterionCode,
            String targetSubAttribute,
            int targetDifficultyRank) {
        this.id = id;
        this.practicePaperId = practicePaperId;
        this.practiceQuestionId = practiceQuestionId;
        this.slotOrder = slotOrder;
        this.targetCriterionCode = targetCriterionCode;
        this.targetSubAttribute = targetSubAttribute;
        this.targetDifficultyRank = targetDifficultyRank;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPracticePaperId() {
        return practicePaperId;
    }

    public UUID getPracticeQuestionId() {
        return practiceQuestionId;
    }

    public int getSlotOrder() {
        return slotOrder;
    }

    public String getTargetCriterionCode() {
        return targetCriterionCode;
    }

    public String getTargetSubAttribute() {
        return targetSubAttribute;
    }

    public int getTargetDifficultyRank() {
        return targetDifficultyRank;
    }
}
