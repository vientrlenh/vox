package com.sep.vox.domain.valueobject;

import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionDifficulty;
import com.sep.vox.domain.model.question.QuestionType;

public record QuestionSelectionSpec(
    QuestionType questionType, 
    QuestionDifficulty difficulty, 
    String targetBandLevel, 
    String skillCode, 
    UUID topicId
) {
    public QuestionSelectionSpec {
        if (questionType == null && difficulty == null && targetBandLevel == null && skillCode == null && topicId == null) {
            throw new IllegalArgumentException("Tiêu chí không được để trống");
        }
        if (skillCode != null && skillCode.isBlank()) {
            throw new IllegalArgumentException("Skill code không được để trống");
        }
        if (targetBandLevel != null && targetBandLevel.isBlank()) {
            throw new IllegalArgumentException("Target band id không được để trống");
        }
    }
}
