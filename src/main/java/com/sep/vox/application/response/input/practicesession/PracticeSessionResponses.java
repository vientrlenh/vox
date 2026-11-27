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
             * The total seconds the student HAS SPOKEN in the session (including this turn) and the speaking
             * budget of the session. The client shows "spoken / budget" from these two numbers.
             *
             * Deliberately NOT elapsed clock time: the quota only deducts the span in which VAD hears
             * sound, so while the AI speaks / the student thinks / grading waits nothing is counted. The screen
             * used to count plain clock time so it looked like counting the quota while actually having
             * nothing to do with the quota.
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
