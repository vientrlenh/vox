package com.sep.vox.domain.service.assessment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;

/**
 * The ONLY place that states "how many seconds a question takes in the paper variant".
 *
 * <p>Two numbers, two different meanings, do not mix them:
 *
 * <ul>
 *   <li>{@code billableSeconds} = prep + maxResponse. This is the part the candidate SPEAKS, i.e. the only part that
 *       can incur AI cost. Only the money estimate uses this number
 *       ({@code ClassTestTokenQuotaGuardService}), because the rate there is in USD per second the candidate
 *       REALLY speaks (see {@code QuotaPricingCalibrationService} -- the calibration denominator is
 *       {@code assessment_item_responses.duration_seconds}).
 *   <li>{@code totalSeconds} = billable + the AUDIO/VIDEO playback duration. This is the real time the candidate
 *       sits in the room, so the countdown clock, the session length and the {@code maxTimePerAttemptMin}
 *       ceiling must all use this number.
 * </ul>
 *
 * <p>Stuffing media into one single number would inflate the money estimate, because media playback seconds are seconds
 * NOBODY speaks and the AI does not run -- multiplying them by that rate would invent a cost that cannot exist.
 *
 * <p>A pure function that does no querying itself: the caller loads the {@link Question} (and {@link QuestionAsset} if it needs
 * {@code totalSeconds}) and passes it in, so each place keeps its own way of gathering data -- some take
 * it from the DB after saving, some project it from the command BEFORE saving so there is nothing in the DB to read yet.
 *
 * <h2>⚠️ Known limitation: editing a question/resource does NOT recompute the paper variant automatically</h2>
 *
 * <p>{@code RecalculateAssessmentTimeDurationService} is only called from operations on the ASSESSMENT / PAPER VARIANT
 * (create/delete a paper variant, edit an item, edit a part of a class test, change the blueprint). No path goes in from
 * {@code usecase/question/}. So if the paper variant HAS already been created and then:
 *
 * <ul>
 *   <li>a real audio/video file is uploaded (the duration changes from the number declared in Excel to the measured number), or
 *   <li>the {@code preparationTimeSeconds} / {@code maxResponseSeconds} of the question is edited,
 * </ul>
 *
 * <p>then {@code paper.timeDurationSeconds} and {@code assessment.assessmentTimeDurationSecond} KEEP THE OLD NUMBER until
 * the next time someone touches the structure of the paper variant.
 *
 * <p>This is PRE-EXISTING behavior (the two time fields of a question were already like this), and adding the
 * media duration only makes it easier to hit -- because uploading files is something very commonly done right after
 * bulk-entering questions.
 *
 * <p><b>How to avoid it:</b> finish the questions (including uploading media) FIRST, then create the paper variant. When
 * {@code CreateAssessmentPaperUseCase} runs it calls recalculate and reads the right number. Decision 2026-08-21:
 * document it rather than automate it yet, because doing it right needs a reverse query question -> item -> paper variant -> assessment
 * on every question and resource edit path.
 */
public final class PaperTimeCalculator {

    private PaperTimeCalculator() {
    }

    public record PaperTimeBreakdown(int totalSeconds, int billableSeconds) {
    }

    /**
     * Only the part that generates AI cost, NOT including the media duration.
     *
     * <p>Made a separate entry instead of letting callers pass an empty map to {@link #breakdownOf}: an empty map would
     * build a breakdown whose {@code totalSeconds} lies (equal to billable, missing media), and a later
     * caller by mistake would have no way to know.
     */
    public static int billableSecondsOf(Collection<Question> questions) {
        int seconds = 0;
        for (Question question : questions) {
            seconds += billableSecondsOfOne(question);
        }
        return seconds;
    }

    /** The original formula, written EXACTLY ONCE in the whole repo. */
    private static int billableSecondsOfOne(Question question) {
        return question.getPreparationTimeSeconds() + question.getMaxResponseSeconds();
    }

    /**
     * Collects the result of {@code QuestionAssetRepository#findByQuestionIdIn} into a map to pass to
     * {@link #breakdownOf}. Each question has at most one asset
     * ({@code uk_question_assets_question_id} is unique on {@code question_id} alone) so a one-to-one map
     * is enough; the merge function only guards against old data that accidentally has two rows, taking the first instead of throwing.
     */
    public static Map<UUID, QuestionAsset> indexByQuestionId(Collection<QuestionAsset> assets) {
        Map<UUID, QuestionAsset> byQuestionId = new HashMap<>();
        for (QuestionAsset asset : assets) {
            byQuestionId.putIfAbsent(asset.getQuestionId(), asset);
        }
        return byQuestionId;
    }

    /**
     * Both numbers. {@code assetByQuestionId} is built with {@link #indexByQuestionId}.
     */
    public static PaperTimeBreakdown breakdownOf(
            Collection<Question> questions,
            Map<UUID, QuestionAsset> assetByQuestionId) {
        int billableSeconds = 0;
        int mediaSeconds = 0;
        for (Question question : questions) {
            billableSeconds += billableSecondsOfOne(question);
            mediaSeconds += mediaSecondsOf(assetByQuestionId.get(question.getId()));
        }
        return new PaperTimeBreakdown(billableSeconds + mediaSeconds, billableSeconds);
    }

    /**
     * Filters by {@code type}, NOT by "durationSeconds is not null": {@code duration_seconds} is a column
     * shared by every asset type, so a stray number on an image would stretch the whole schedule. Images and
     * paragraphs are shown throughout the preparation time so they are already inside {@code preparationTimeSeconds}.
     */
    public static int mediaSecondsOf(QuestionAsset asset) {
        if (asset == null || asset.getDurationSeconds() == null) {
            return 0;
        }
        if (asset.getType() != QuestionAssetType.AUDIO && asset.getType() != QuestionAssetType.VIDEO) {
            return 0;
        }
        return Math.max(0, asset.getDurationSeconds());
    }
}
