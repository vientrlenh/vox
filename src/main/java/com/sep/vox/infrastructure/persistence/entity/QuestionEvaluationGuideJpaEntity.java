package com.sep.vox.infrastructure.persistence.entity;

import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_evaluation_guides", indexes = {
    @Index(columnList = "question_id", name = "idx_question_evaluation_guides_question", unique = true)
})
public class QuestionEvaluationGuideJpaEntity {
    
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

    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    @Column(name = "expected_content", columnDefinition = "TEXT")
    private String expectedContent;

    @Column(name = "key_points", columnDefinition = "TEXT")
    private String keyPoints;

    @Column(name = "acceptable_responses", columnDefinition = "TEXT")
    private String acceptableResponses;

    @Column(name = "off_topic_examples", columnDefinition = "TEXT")
    private String offTopicExamples;

    @Column(name = "scoring_hints", columnDefinition = "TEXT")
    private String scoringHints;

    @Column(name = "common_mistakes", columnDefinition = "TEXT")
    private String commonMistakes;


    protected QuestionEvaluationGuideJpaEntity() {}


    public QuestionEvaluationGuideJpaEntity(UUID id, UUID questionId, String expectedContent, String keyPoints,
            String acceptableResponses, String offTopicExamples, String scoringHints, String commonMistakes) {
        this.id = id;
        this.questionId = questionId;
        this.expectedContent = expectedContent;
        this.keyPoints = keyPoints;
        this.acceptableResponses = acceptableResponses;
        this.offTopicExamples = offTopicExamples;
        this.scoringHints = scoringHints;
        this.commonMistakes = commonMistakes;
    }


    public UUID getId() {
        return id;
    }


    public void setId(UUID id) {
        this.id = id;
    }


    public UUID getQuestionId() {
        return questionId;
    }


    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }


    public String getExpectedContent() {
        return expectedContent;
    }


    public void setExpectedContent(String expectedContent) {
        this.expectedContent = expectedContent;
    }


    public String getKeyPoints() {
        return keyPoints;
    }


    public void setKeyPoints(String keyPoints) {
        this.keyPoints = keyPoints;
    }


    public String getAcceptableResponses() {
        return acceptableResponses;
    }


    public void setAcceptableResponses(String acceptableResponses) {
        this.acceptableResponses = acceptableResponses;
    }


    public String getOffTopicExamples() {
        return offTopicExamples;
    }


    public void setOffTopicExamples(String offTopicExamples) {
        this.offTopicExamples = offTopicExamples;
    }


    public String getScoringHints() {
        return scoringHints;
    }


    public void setScoringHints(String scoringHints) {
        this.scoringHints = scoringHints;
    }


    public String getCommonMistakes() {
        return commonMistakes;
    }


    public void setCommonMistakes(String commonMistakes) {
        this.commonMistakes = commonMistakes;
    }

    

}
