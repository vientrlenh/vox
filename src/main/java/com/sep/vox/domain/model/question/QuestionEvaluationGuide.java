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

    public static QuestionEvaluationGuide forQuestion(UUID questionId, String expectedContent, String keyPoints,
                                                      String acceptableResponses, String offTopicExamples,
                                                      String scoringHints, String commonMistakes) {
        return builder()
                .questionId(questionId)
                .expectedContent(expectedContent)
                .keyPoints(keyPoints)
                .acceptableResponses(acceptableResponses)
                .offTopicExamples(offTopicExamples)
                .scoringHints(scoringHints)
                .commonMistakes(commonMistakes)
                .build();
    }

    public static QuestionEvaluationGuide emptyFor(UUID questionId) {
        return forQuestion(questionId, null, null, null, null, null, null);
    }

    public QuestionEvaluationGuide copyTo(UUID targetQuestionId) {
        return forQuestion(targetQuestionId, expectedContent, keyPoints, acceptableResponses, offTopicExamples,
                scoringHints, commonMistakes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final QuestionEvaluationGuide target = new QuestionEvaluationGuide();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder questionId(UUID questionId) {
            target.questionId = questionId;
            return this;
        }

        public Builder expectedContent(String expectedContent) {
            target.expectedContent = expectedContent;
            return this;
        }

        public Builder keyPoints(String keyPoints) {
            target.keyPoints = keyPoints;
            return this;
        }

        public Builder acceptableResponses(String acceptableResponses) {
            target.acceptableResponses = acceptableResponses;
            return this;
        }

        public Builder offTopicExamples(String offTopicExamples) {
            target.offTopicExamples = offTopicExamples;
            return this;
        }

        public Builder scoringHints(String scoringHints) {
            target.scoringHints = scoringHints;
            return this;
        }

        public Builder commonMistakes(String commonMistakes) {
            target.commonMistakes = commonMistakes;
            return this;
        }

        public QuestionEvaluationGuide build() {
            return target;
        }
    }
}
