package com.sep.vox.application.response.input.practicesession;

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
            String startedAt,
            String endedAt,
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
            String startedAt,
            int durationSeconds,
            int itemCount,
            Double overallScore,
            List<CriterionScore> criterionScores,
            boolean completed,
            int pendingEvaluationCount,
            Double difficultyRank,
            List<TeacherPracticeTurnView> turns,
            /** Thang chấm của CHÍNH phiên này -- 0-100 từ V13, thang rubric với phiên cũ. */
            double scoreScaleMin,
            double scoreScaleMax) {
    }
}
