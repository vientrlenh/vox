package com.sep.vox.domain.service.personalization;

import java.util.List;
import java.util.Map;

/**
 * The target tense of a practice question: where it is chosen, and which tense is allowed at which band.
 *
 * <p>This replaces targeting the weakness sub-label (removed). It used to say "the student is weak at
 * tense_control", now it says "this slot practises the past" -- more precise, and the source is the TOPIC and not
 * the student profile.
 *
 * <p><b>What cannot be forced by instruction is forced by construction.</b> Telling the model "write a simple past practice question" makes
 * it write a sentence <em>about</em> the past that the student can still answer in the present. What can be forced
 * is the time marker inside the question itself -- "What <b>did</b> you do last weekend?" makes the past
 * mandatory. That constraint lives in the sentence-writing prompt on the Python side; this class only decides
 * <em>which tense to aim at</em>.
 */
public final class TensePolicy {

    public static final String PRESENT = "PRESENT";
    public static final String PAST = "PAST";
    public static final String FUTURE = "FUTURE";
    public static final String PERFECT = "PERFECT";
    public static final String CONDITIONAL = "CONDITIONAL";

    /** A topic leaning entirely to one side of time -- attached by topicGenerationGraph when composing. */
    public static final String AFFORDANCE_PAST = "PAST";
    public static final String AFFORDANCE_FUTURE = "FUTURE";
    public static final String AFFORDANCE_MIXED = "MIXED";

    /**
     * The coarse Robinson difficulty scale only produces 6 levels -- see {@code raw_difficulty} on the
     * Python side. These two constants must match it.
     */
    private static final int RAW_MIN = 1;
    private static final int RAW_MAX = 6;

    /**
     * The LOWEST coarse difficulty that a question with this tense can reach. Derived directly from the formula
     * {@code raw = 1 + (not here_and_now) + (num_elements >= 4) + reasoning_weight
     * + (abstractness == "abstract")}:
     *
     * <ul>
     *   <li>past / future / perfect ⇒ the question is NOT anchored in the present ⇒ {@code +1}</li>
     *   <li>conditional ⇒ {@code reasoning_type = hypothetical} ⇒ {@code +2}, plus
     *       {@code here_and_now = False} as well</li>
     * </ul>
     *
     * <p>Why block it: asking for {@code rank = 1} together with {@code CONDITIONAL} is a SELF-CONTRADICTORY
     * request -- no combination of Robinson features satisfies both. Without blocking, either the question is dropped
     * at a later stage (wasting an LLM call), or worse, slips into the pool with the wrong {@code rank}.
     */
    private static final Map<String, Integer> RAW_FLOOR = Map.of(
        PRESENT, 1,
        PAST, 2,
        FUTURE, 2,
        PERFECT, 2,
        CONDITIONAL, 4
    );

    /**
     * Rotation order for a {@code MIXED} topic: easy first, hard after. Only a priority order when
     * spreading across slots, not a score.
     */
    private static final List<String> ROTATION = List.of(PRESENT, PAST, FUTURE, PERFECT, CONDITIONAL);

    private TensePolicy() {
    }

    /**
     * The tenses this band can reach. Never empty: {@link #PRESENT} has a floor of 1 so it is
     * always valid.
     */
    public static List<String> allowedFor(int targetRank, int bandCount) {
        int raw = rawForRank(targetRank, bandCount);
        return ROTATION.stream()
            .filter(tense -> RAW_FLOOR.get(tense) <= raw)
            .toList();
    }

    /**
     * The tense for slot {@code slotIndex} of the session.
     *
     * <p>The topic speaks first, rotation after. For a topic "My school's history", if slot 2 demands the future
     * the generated question is awkward -- <em>"What will your school's history be like?"</em> is meaningless;
     * blind rotation does not know that.
     *
     * <p>The cycle is FIXED, not random, by the same principle as
     * {@code PracticeFocusInfo.criterionForSlot}: the same student at the same slot always gets the same tense, so
     * bugs are reproducible and "one session covers many time frames" is certain to happen rather
     * than a matter of luck.
     *
     * <p>A topic leaning to a tense the band does not allow (e.g. a history topic at band 1, where only
     * {@link #PRESENT} is valid) lets the band WIN: fall back to rotating within the valid set. Forcing the
     * target would produce a question that contradicts itself on difficulty, while asking about a history topic in the present
     * tense is still natural.
     */
    public static String forSlot(
            String temporalAffordance,
            int slotIndex,
            int targetRank,
            int bandCount) {
        List<String> allowed = allowedFor(targetRank, bandCount);
        String preferred = switch (temporalAffordance == null ? AFFORDANCE_MIXED : temporalAffordance) {
            case AFFORDANCE_PAST -> PAST;
            case AFFORDANCE_FUTURE -> FUTURE;
            default -> null;
        };
        if (preferred != null && allowed.contains(preferred)) {
            return preferred;
        }
        return allowed.get(Math.max(0, slotIndex) % allowed.size());
    }

    /**
     * Framework band -> coarse Robinson difficulty. The inverse of {@code difficulty_rank} on the Python side
     * ({@code raw_for_rank}); for a 6-band scale it is the identity.
     *
     * <p>Not hard-coded to 6: if a school switches to IELTS 9 bands the constant 6 would state the floor of
     * every tense wrongly without any error.
     */
    private static int rawForRank(int rank, int bandCount) {
        int safeBandCount = Math.max(1, bandCount);
        int safeRank = Math.max(1, Math.min(safeBandCount, rank));
        if (safeBandCount == RAW_MAX) {
            return safeRank;
        }
        if (safeBandCount == 1) {
            return RAW_MIN;
        }
        double normalized = (double) (safeRank - 1) / (safeBandCount - 1);
        int raw = RAW_MIN + (int) Math.round(normalized * (RAW_MAX - RAW_MIN));
        return Math.max(RAW_MIN, Math.min(RAW_MAX, raw));
    }
}
