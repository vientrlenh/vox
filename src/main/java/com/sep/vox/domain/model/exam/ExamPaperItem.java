package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.util.UUID;

public class ExamPaperItem {
    private UUID id;
    private UUID sectionId;
    private UUID paperId;
    private UUID questionId;
    private int order;
    private BigDecimal weight; 

    public ExamPaperItem() {}

    public ExamPaperItem(UUID id, UUID sectionId, UUID paperId, UUID questionId, int order, BigDecimal weight) {
        this.id = id;
        this.sectionId = sectionId;
        this.paperId = paperId;
        this.questionId = questionId;
        this.order = order;
        this.weight = weight;
    }

    public ExamPaperItem(UUID sectionId, UUID paperId, UUID questionId, int order, BigDecimal weight) {
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

    
}
