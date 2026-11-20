package com.sep.vox.application.response.input.practicesession;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
            Instant startedAt,
            Instant endedAt,
            int pendingEvaluations) {
    }

    public record TurnCorrection(
            String category,
            String originalText,
            String correctedText,
            String explanation,
            String correctAudioUrl) {
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
    }

    public record TeacherPracticeTurnView(
            int turnOrder,
            String transcript,
            String audioUrl,
            String wordFeedbackJson,
            Double turnScore,
            List<TurnCorrection> corrections) {
    }

    public record CriterionScore(
            String criterionCode,
            Double score) {
    }

    public record PracticeDashboardStats(
            int sessionsDone,
            double averageScore,
            int streakDays) {
    }

    public record TeacherPracticeSessionDetail(
            UUID sessionId,
            String topicName,
            Instant startedAt,
            int durationSeconds,
            int itemCount,
            Double overallScore,
            List<CriterionScore> criterionScores,
            boolean completed,
            int pendingEvaluationCount,
            int gradingGaveUpCount,
            Double difficultyRank,
            List<TeacherPracticeTurnView> turns,
            /** The scoring scale of THIS session itself -- 0-100 from V13, the rubric scale for older sessions. */
            double scoreScaleMin,
            double scoreScaleMax) {
    }
}
