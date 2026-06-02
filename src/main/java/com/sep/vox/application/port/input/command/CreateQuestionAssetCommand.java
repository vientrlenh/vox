package com.sep.vox.application.port.input.command;

public record CreateQuestionAssetCommand(
    String title, 
    Integer durationSeconds,
    String altText, 
    String type, 
    String url, 
    String transcript,
    String description, 
    int order
) {
    
}
