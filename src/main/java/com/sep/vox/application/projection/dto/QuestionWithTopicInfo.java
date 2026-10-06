package com.sep.vox.application.projection.dto;

public interface QuestionWithTopicInfo {

    String getQuestionText();

    String getEvaluationGuideJson();

    /** SHORT_ANSWER | LONG_ANSWER | DESCRIPTION | OPINION. */
    String getQuestionType();

    Integer getMinResponseSeconds();

    Integer getMaxResponseSeconds();

    String getTopicName();

    String getTopicDescription();
}
