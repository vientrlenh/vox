package com.sep.vox.domain.model.question;

import java.util.UUID;

public class QuestionEvaluationGuide {
    private UUID id;
    private UUID questionId;
    private String expectedContent;
    private String keyPoints;
    private String acceptableResponses;
    private String offTopicExamples;
    private String scoringHints;
    private String commonMistakes;

    public QuestionEvaluationGuide() {
    }

    public QuestionEvaluationGuide(UUID id, UUID questionId, String expectedContent, String keyPoints,
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




    public QuestionEvaluationGuide(UUID questionId, String expectedContent, String keyPoints,
            String acceptableResponses, String offTopicExamples, String scoringHints, String commonMistakes) {
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
