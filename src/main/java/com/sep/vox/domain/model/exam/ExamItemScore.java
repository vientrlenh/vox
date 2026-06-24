package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class ExamItemScore {
    private UUID id;
    private UUID responseId;
    private UUID paperItemId;
    private Map<String, BigDecimal> rubricScores;
    private BigDecimal itemScore;
    private String gradedByModel;
    private OffsetDateTime gradedAt;
    private ExamItemScoreStatus status; 

    public ExamItemScore() {}

    public ExamItemScore(UUID id, UUID responseId, UUID paperItemId, Map<String, BigDecimal> rubricScores,
            BigDecimal itemScore, String gradedByModel, OffsetDateTime gradedAt, ExamItemScoreStatus status) {
        this.id = id;
        this.responseId = responseId;
        this.paperItemId = paperItemId;
        this.rubricScores = rubricScores;
        this.itemScore = itemScore;
        this.gradedByModel = gradedByModel;
        this.gradedAt = gradedAt;
        this.status = status;
    }

    public ExamItemScore(UUID responseId, UUID paperItemId, Map<String, BigDecimal> rubricScores, BigDecimal itemScore,
            String gradedByModel, OffsetDateTime gradedAt, ExamItemScoreStatus status) {
        this.responseId = responseId;
        this.paperItemId = paperItemId;
        this.rubricScores = rubricScores;
        this.itemScore = itemScore;
        this.gradedByModel = gradedByModel;
        this.gradedAt = gradedAt;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getResponseId() {
        return responseId;
    }

    public void setResponseId(UUID responseId) {
        this.responseId = responseId;
    }

    public UUID getPaperItemId() {
        return paperItemId;
    }

    public void setPaperItemId(UUID paperItemId) {
        this.paperItemId = paperItemId;
    }

    public Map<String, BigDecimal> getRubricScores() {
        return rubricScores;
    }

    public void setRubricScores(Map<String, BigDecimal> rubricScores) {
        this.rubricScores = rubricScores;
    }

    public BigDecimal getItemScore() {
        return itemScore;
    }

    public void setItemScore(BigDecimal itemScore) {
        this.itemScore = itemScore;
    }

    public String getGradedByModel() {
        return gradedByModel;
    }

    public void setGradedByModel(String gradedByModel) {
        this.gradedByModel = gradedByModel;
    }

    public OffsetDateTime getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(OffsetDateTime gradedAt) {
        this.gradedAt = gradedAt;
    }

    public ExamItemScoreStatus getStatus() {
        return status;
    }

    public void setStatus(ExamItemScoreStatus status) {
        this.status = status;
    }

    
}
