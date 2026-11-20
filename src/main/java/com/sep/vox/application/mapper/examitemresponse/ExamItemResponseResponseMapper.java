package com.sep.vox.application.mapper.examitemresponse;

import java.util.List;

import com.sep.vox.application.response.input.examitemresponse.ExamItemResponseDetailsResponse;
import com.sep.vox.application.response.input.examitemresponse.ExamItemResponseTurnResponse;
import com.sep.vox.domain.model.exam.ExamItemResponse;
import com.sep.vox.domain.model.exam.ExamItemResponseTurn;

public final class ExamItemResponseResponseMapper {

    private ExamItemResponseResponseMapper() {
    }

    public static ExamItemResponseDetailsResponse toDetailsResponse(
            ExamItemResponse response,
            List<ExamItemResponseTurn> turns) {
        return new ExamItemResponseDetailsResponse(
            response.getId(),
            response.getSessionId(),
            response.getPaperItemId(),
            response.getAudioUrl(),
            response.getDurationSeconds(),
            response.getTranscript(),
            response.getSubmittedAt() == null ? null : response.getSubmittedAt().toString(),
            turns.stream().map(ExamItemResponseResponseMapper::toTurnResponse).toList()
        );
    }

    public static ExamItemResponseTurnResponse toTurnResponse(ExamItemResponseTurn turn) {
        return new ExamItemResponseTurnResponse(
            turn.getId(),
            turn.getExamItemResponseId(),
            turn.getTurnOrder(),
            turn.getTurnType().name(),
            turn.getPromptText(),
            turn.getAudioUrl(),
            turn.getTranscript(),
            turn.getDurationSeconds(),
            turn.getWordCount(),
            turn.getAnsweredAt() == null ? null : turn.getAnsweredAt().toString(),
            turn.getCreatedAt() == null ? null : turn.getCreatedAt().toString()
        );
    }
}
