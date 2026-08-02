package com.sep.vox.application.query.dto;

public interface QuestionWithTopicInfo {

    String getQuestionText();

    String getEvaluationGuideJson();

    Integer getMaxResponseSeconds();

    String getTopicName();

    String getTopicDescription();
}
