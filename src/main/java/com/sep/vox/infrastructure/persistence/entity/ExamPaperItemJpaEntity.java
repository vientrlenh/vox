package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_paper_items")
public class ExamPaperItemJpaEntity {
    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id", 
        nullable = false, 
        updatable = false, 
        insertable = false, 
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "blueprint_slot_id", updatable = false)
    private UUID blueprintSlotId;

    @Column(name = "section_id", updatable = false)
    private UUID sectionId;

    @Column(name = "paper_id", updatable = false)
    private UUID paperId; 
    
    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "item_order", nullable = false)
    private int order;

    @Column(name = "weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    protected ExamPaperItemJpaEntity() {}

    public ExamPaperItemJpaEntity(UUID id, UUID blueprintSlotId, UUID sectionId, UUID paperId, UUID questionId, int order,
            BigDecimal weight) {
        this.id = id;
        this.blueprintSlotId = blueprintSlotId;
        this.sectionId = sectionId;
        this.paperId = paperId;
        this.questionId = questionId;
        this.order = order;
        this.weight = weight;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSectionId() {
        return sectionId;
    }

    public void setSectionId(UUID sectionId) {
        this.sectionId = sectionId;
    }

    public UUID getPaperId() {
        return paperId;
    }

    public void setPaperId(UUID paperId) {
        this.paperId = paperId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public UUID getBlueprintSlotId() {
        return blueprintSlotId;
    }

    public void setBlueprintSlotId(UUID blueprintSlotId) {
        this.blueprintSlotId = blueprintSlotId;
    }

    
}
