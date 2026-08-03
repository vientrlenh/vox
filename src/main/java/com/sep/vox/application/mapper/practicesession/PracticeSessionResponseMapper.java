package com.sep.vox.application.mapper.practicesession;

import java.util.List;

import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.CriterionScore;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.PracticeSession;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.SubmitTurnResult;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.TeacherPracticeSessionDetail;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.TeacherPracticeTurnView;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.TurnCorrection;
import com.sep.vox.domain.dto.personalization.PracticeCriterionScoreDto;
import com.sep.vox.domain.dto.personalization.PracticeSessionDto;
import com.sep.vox.domain.dto.personalization.SubmitTurnResultDto;
import com.sep.vox.domain.dto.personalization.TeacherPracticeSessionDetailDto;
import com.sep.vox.domain.dto.personalization.TeacherPracticeTurnViewDto;
import com.sep.vox.domain.dto.personalization.TurnCorrectionDto;

public final class PracticeSessionResponseMapper {

    private PracticeSessionResponseMapper() {
    }

    public static PracticeSession toResponse(PracticeSessionDto dto) {
        if (dto == null) {
            return null;
        }
        return new PracticeSession(
            dto.id(),
            dto.paperId(),
            dto.topicId(),
            dto.topicName(),
            dto.origin(),
            dto.status(),
            dto.abandonDiagnosis(),
            dto.overallScore(),
            dto.gradedSeconds(),
            dto.startedAt(),
            dto.endedAt()
        );
    }

    public static List<PracticeSession> toResponseList(List<PracticeSessionDto> dtos) {
        return dtos.stream().map(PracticeSessionResponseMapper::toResponse).toList();
    }

    private static TurnCorrection toResponse(TurnCorrectionDto dto) {
        return new TurnCorrection(
            dto.category(),
            dto.originalText(),
            dto.correctedText(),
            dto.explanation(),
            dto.correctAudioUrl()
        );
    }

    public static SubmitTurnResult toResponse(SubmitTurnResultDto dto) {
        return new SubmitTurnResult(
            dto.responseId(),
            dto.turnId(),
            dto.remainingGradedSeconds(),
            dto.evaluationQueued(),
            dto.corrections().stream().map(PracticeSessionResponseMapper::toResponse).toList(),
            dto.quotaExhausted(),
            dto.sessionSpokenSeconds(),
            dto.sessionBudgetSeconds()
        );
    }

    private static TeacherPracticeTurnView toResponse(TeacherPracticeTurnViewDto dto) {
        return new TeacherPracticeTurnView(
            dto.turnOrder(),
            dto.transcript(),
            dto.audioUrl(),
            dto.wordFeedbackJson(),
            dto.turnScore(),
            dto.corrections().stream().map(PracticeSessionResponseMapper::toResponse).toList()
        );
    }

    private static CriterionScore toResponse(PracticeCriterionScoreDto dto) {
        return new CriterionScore(dto.criterionCode(), dto.score(), dto.matchedBandCode());
    }

    public static TeacherPracticeSessionDetail toResponse(TeacherPracticeSessionDetailDto dto) {
        return new TeacherPracticeSessionDetail(
            dto.sessionId(),
            dto.topicName(),
            dto.startedAt(),
            dto.durationSeconds(),
            dto.itemCount(),
            dto.overallScore(),
            dto.criterionScores().stream().map(PracticeSessionResponseMapper::toResponse).toList(),
            dto.completed(),
            dto.turns().stream().map(PracticeSessionResponseMapper::toResponse).toList()
        );
    }
}
