package com.sep.vox.domain.mapper;

import java.util.List;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import com.sep.vox.domain.dto.personalization.PracticePaperDto;
import com.sep.vox.domain.dto.personalization.PracticePaperQuestionDto;
import com.sep.vox.domain.model.personalization.PracticePaper;
import com.sep.vox.domain.model.personalization.PracticeQuestion;

public final class PracticePaperDtoMapper {

    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    private PracticePaperDtoMapper() {
    }

    public static PracticePaperDto toDto(
            PracticePaper paper,
            List<PracticeQuestion> questions,
            int sessionBudgetSeconds) {
        var responseQuestions = java.util.stream.IntStream
            .range(0, questions.size())
            .mapToObj(index -> toDto(questions.get(index), index + 1))
            .toList();
        return new PracticePaperDto(
            paper.getId(),
            paper.getPracticeTopicId(),
            paper.getOrigin(),
            paper.getPlannedSeconds(),
            paper.getReservedQuotaSeconds(),
            sessionBudgetSeconds,
            responseQuestions
        );
    }

    private static PracticePaperQuestionDto toDto(
            PracticeQuestion question,
            int slot) {
        return new PracticePaperQuestionDto(
            question.getId(),
            slot,
            question.getQuestionText(),
            question.getTargetCriterionCode(),
            question.getTargetSubAttribute(),
            question.getDifficultyRank(),
            question.getMaxResponseSeconds(),
            question.getMinResponseSeconds(),
            parseIdeas(question.getSuggestedIdeasJson())
        );
    }

    private static List<String> parseIdeas(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return JSON_MAPPER.readValue(value, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }
}
