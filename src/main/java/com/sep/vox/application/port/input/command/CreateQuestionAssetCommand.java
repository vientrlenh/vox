package com.sep.vox.application.port.input.command;

import com.sep.vox.domain.model.question.QuestionAssetType;

public record CreateQuestionAssetCommand(
    String title, 
    Integer durationSeconds,
    String altText, 
    QuestionAssetType type, 
    String url, 
    String transcript,
    String description, 
    int order
) {
    
}
