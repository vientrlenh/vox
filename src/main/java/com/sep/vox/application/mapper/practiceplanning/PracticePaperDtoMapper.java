package com.sep.vox.application.mapper.practiceplanning;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.dto.personalization.PracticePaperDto;
import com.sep.vox.domain.dto.personalization.PracticePaperQuestionDto;
import com.sep.vox.domain.model.personalization.PracticePaper;
import com.sep.vox.domain.model.personalization.PracticeQuestion;

@Service
public class PracticePaperDtoMapper {

    private final JsonSerializationPort jsonSerializationPort;

    public PracticePaperDtoMapper(JsonSerializationPort jsonSerializationPort) {
        this.jsonSerializationPort = jsonSerializationPort;
    }

    public PracticePaperDto toDto(
            PracticePaper paper,
            List<PracticeQuestion> questions,
            int sessionBudgetSeconds) {
        List<PracticePaperQuestionDto> responseQuestions = java.util.stream.IntStream
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

    private PracticePaperQuestionDto toDto(
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

    private List<String> parseIdeas(String value) {
        try {
            return jsonSerializationPort.toStringList(value);
        } catch (IllegalStateException exception) {
            return List.of();
        }
    }
}
