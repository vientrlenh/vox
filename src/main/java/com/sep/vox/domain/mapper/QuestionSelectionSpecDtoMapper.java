package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.QuestionSelectionSpecDto;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

public final class QuestionSelectionSpecDtoMapper {

    private QuestionSelectionSpecDtoMapper() {
    }

    public static QuestionSelectionSpecDto toDto(QuestionSelectionSpec domain) {
        if (domain == null) {
            return null;
        }
        return new QuestionSelectionSpecDto(
            domain.questionType() == null ? null : domain.questionType().name(),
            domain.difficulty() == null ? null : domain.difficulty().name(),
            domain.targetBandLevel(),
            domain.skillCode(),
            domain.topicId()
        );
    }
}
