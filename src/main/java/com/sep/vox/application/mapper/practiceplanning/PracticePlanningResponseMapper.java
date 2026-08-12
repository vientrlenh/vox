package com.sep.vox.application.mapper.practiceplanning;

import java.util.List;

import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaper;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaperQuestion;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.TopicSearchResult;
import com.sep.vox.domain.dto.personalization.PracticePaperDto;
import com.sep.vox.domain.dto.personalization.PracticePaperQuestionDto;
import com.sep.vox.domain.dto.personalization.PracticeTopicOfferDto;
import com.sep.vox.domain.dto.personalization.TopicSearchResultDto;

public final class PracticePlanningResponseMapper {

    private PracticePlanningResponseMapper() {
    }

    public static PracticeTopicOffer toResponse(PracticeTopicOfferDto dto) {
        if (dto == null) {
            return null;
        }
        return new PracticeTopicOffer(dto.topicId(), dto.name(), dto.dimension(), dto.savedByMe());
    }

    public static List<PracticeTopicOffer> toResponseList(List<PracticeTopicOfferDto> dtos) {
        return dtos.stream().map(PracticePlanningResponseMapper::toResponse).toList();
    }

    public static TopicSearchResult toResponse(TopicSearchResultDto dto) {
        return new TopicSearchResult(toResponseList(dto.topics()), dto.canGenerate());
    }

    public static PracticePaper toResponse(PracticePaperDto dto) {
        return new PracticePaper(
            dto.id(),
            dto.topicId(),
            dto.origin(),
            dto.plannedSeconds(),
            dto.reservedQuotaSeconds(),
            dto.sessionBudgetSeconds(),
            dto.questions().stream().map(PracticePlanningResponseMapper::toResponse).toList()
        );
    }

    private static PracticePaperQuestion toResponse(PracticePaperQuestionDto dto) {
        return new PracticePaperQuestion(
            dto.questionId(),
            dto.slot(),
            dto.questionText(),
            dto.criterionCode(),
            dto.subAttribute(),
            dto.difficultyRank(),
            dto.maxResponseSeconds(),
            dto.minResponseSeconds(),
            dto.suggestedIdeas()
        );
    }
}
