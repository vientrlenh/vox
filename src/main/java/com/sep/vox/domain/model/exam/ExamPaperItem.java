package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.util.UUID;

public class ExamPaperItem {
    private UUID id;
    private UUID blueprintSlotId;
    private UUID sectionId;
    private UUID paperId;
    private UUID questionId;
    private int order;
    private BigDecimal weight; 

    public ExamPaperItem() {}

    public ExamPaperItem(UUID id, UUID blueprintSlotId, UUID sectionId, UUID paperId, UUID questionId, int order, BigDecimal weight) {
        this.id = id;
        this.blueprintSlotId = blueprintSlotId;
        this.sectionId = sectionId;
        this.paperId = paperId;
        this.questionId = questionId;
        this.order = order;
        this.weight = weight;
    }

    public ExamPaperItem(UUID blueprintSlotId, UUID sectionId, UUID paperId, UUID questionId, int order, BigDecimal weight) {
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
