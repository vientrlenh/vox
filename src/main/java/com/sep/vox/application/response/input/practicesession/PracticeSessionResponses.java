package com.sep.vox.application.response.input.practicesession;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.dto.personalization.PracticeCriterionScoreDto;
import com.sep.vox.domain.dto.personalization.PracticeSessionDto;
import com.sep.vox.domain.dto.personalization.SubmitTurnResultDto;
import com.sep.vox.domain.dto.personalization.TeacherPracticeSessionDetailDto;
import com.sep.vox.domain.dto.personalization.TeacherPracticeTurnViewDto;
import com.sep.vox.domain.dto.personalization.TurnCorrectionDto;

public final class PracticeSessionResponses {

    private PracticeSessionResponses() {
    }

    public record PracticeSession(
            UUID id,
            UUID paperId,
            UUID topicId,
            String topicName,
            String origin,
            String status,
            String abandonDiagnosis,
            Double overallScore,
            int gradedSeconds,
            String startedAt,
            String endedAt,
            int pendingEvaluations) {

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
                dto.endedAt(),
                dto.pendingEvaluations()
            );
        }

        public static List<PracticeSession> toResponseList(List<PracticeSessionDto> dtos) {
            return dtos.stream()
                .map(PracticeSession::toResponse)
                .toList();
        }
    }

    public record TurnCorrection(
            String category,
            String originalText,
            String correctedText,
            String explanation,
            String correctAudioUrl) {

        public static TurnCorrection toResponse(TurnCorrectionDto dto) {
            return new TurnCorrection(
                dto.category(),
                dto.originalText(),
                dto.correctedText(),
                dto.explanation(),
                dto.correctAudioUrl()
            );
        }
    }

    public record SubmitTurnResult(
            UUID responseId,
            UUID turnId,
            int remainingGradedSeconds,
            boolean evaluationQueued,
            List<TurnCorrection> corrections,
            boolean quotaExhausted,
            /**
             * Tổng số giây học sinh ĐÃ NÓI trong phiên (đã cộng cả lượt này) và ngân sách nói
             * của phiên. Client hiện "đã nói / ngân sách" từ hai số này.
             *
             * Cố ý KHÔNG phải thời gian trôi trên đồng hồ: quota chỉ trừ đúng khoảng VAD nghe
             * thấy tiếng, nên lúc AI nói / học sinh nghĩ / chờ chấm đều không tính. Màn hình
             * trước đây đếm đồng hồ suông nên trông như đang đếm hạn mức mà thật ra không liên
             * quan gì tới hạn mức.
             */
            int sessionSpokenSeconds,
            int sessionBudgetSeconds) {

        public static SubmitTurnResult toResponse(SubmitTurnResultDto dto) {
            return new SubmitTurnResult(
                dto.responseId(),
                dto.turnId(),
                dto.remainingGradedSeconds(),
                dto.evaluationQueued(),
                dto.corrections().stream().map(TurnCorrection::toResponse).toList(),
                dto.quotaExhausted(),
                dto.sessionSpokenSeconds(),
                dto.sessionBudgetSeconds()
            );
        }
    }

    public record TeacherPracticeTurnView(
            int turnOrder,
            String transcript,
            String audioUrl,
            String wordFeedbackJson,
            Double turnScore,
            List<TurnCorrection> corrections) {

        public static TeacherPracticeTurnView toResponse(TeacherPracticeTurnViewDto dto) {
            return new TeacherPracticeTurnView(
                dto.turnOrder(),
                dto.transcript(),
                dto.audioUrl(),
                dto.wordFeedbackJson(),
                dto.turnScore(),
                dto.corrections().stream().map(TurnCorrection::toResponse).toList()
            );
        }
    }

    public record CriterionScore(
            String criterionCode,
            Double score) {

        public static CriterionScore toResponse(PracticeCriterionScoreDto dto) {
            return new CriterionScore(dto.criterionCode(), dto.score());
        }
    }

    public record PracticeDashboardStats(
            int sessionsDone,
            double averageScore,
            int streakDays) {
    }

    public record TeacherPracticeSessionDetail(
            UUID sessionId,
            String topicName,
            String startedAt,
            int durationSeconds,
            int itemCount,
            Double overallScore,
            List<CriterionScore> criterionScores,
            boolean completed,
            int pendingEvaluationCount,
            int gradingGaveUpCount,
            Double difficultyRank,
            List<TeacherPracticeTurnView> turns,
            /** Thang chấm của CHÍNH phiên này -- 0-100 từ V13, thang rubric với phiên cũ. */
            double scoreScaleMin,
            double scoreScaleMax) {

        public static TeacherPracticeSessionDetail toResponse(TeacherPracticeSessionDetailDto dto) {
            return new TeacherPracticeSessionDetail(
                dto.sessionId(),
                dto.topicName(),
                dto.startedAt(),
                dto.durationSeconds(),
                dto.itemCount(),
                dto.overallScore(),
                dto.criterionScores().stream().map(CriterionScore::toResponse).toList(),
                dto.completed(),
                dto.pendingEvaluationCount(),
                dto.gradingGaveUpCount(),
                dto.difficultyRank(),
                dto.turns().stream().map(TeacherPracticeTurnView::toResponse).toList(),
                // Forwarded as-is: the scale is already computed in the query (COALESCE on the
                // LEFT JOIN of rubric_versions), nothing is looked up here.
                dto.scoreScaleMin(),
                dto.scoreScaleMax()
            );
        }
    }
}
