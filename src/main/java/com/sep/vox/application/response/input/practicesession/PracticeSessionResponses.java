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
            String endedAt) {
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
            List<TurnCorrection> corrections) {
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
            Double score,
            String matchedBandCode) {
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
            List<TeacherPracticeTurnView> turns) {
    }
}
