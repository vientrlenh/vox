package com.sep.vox.domain.service.personalization;

public final class SessionDiagnosisPolicy {
    // Two parts in one file because they answer the SAME question: what the session just now says
    // about the student's interests. diagnose() handles the "why they gave up" part, signal() handles "how strong
    // it is", and both flow into InterestVectorService.recordSessionOutcome. Split into two
    // classes, every adjustment of the signal levels would mean opening two files to cross-check the table of values.

    private SessionDiagnosisPolicy() {
    }

    /**
     * Infers the reason a student abandoned a practice session, based on the latest score and the signals of
     * asking for help / long pauses during the session.
     *
     * <p><b>The two behaviour parameters now really have data.</b> Previously the client hard-coded
     * {@code 0} for both, so this rule degenerated into a pure score threshold: the branch
     * {@code helpRequestCount == 0 && longPauseCount <= 1} was always true and the branch
     * {@code helpRequestCount >= 1 || longPauseCount >= 2} always false, i.e. "bored" was inferred from a
     * HIGH SCORE and not from behaviour. Current sources:
     *
     * <ul>
     *   <li>{@code helpRequestCount} — the number of times the student pressed the "Hint" button in the session. An
     *       active signal: the student themself says they are stuck.</li>
     *   <li>{@code longPauseCount} — the number of turns in which, after the AI finished reading the question, VAD reported no
     *       {@code vad_speech_start} within 10 seconds. Counted at most once per turn.</li>
     * </ul>
     *
     * <p>Only called on the path where the student ends the session THEMSELVES. The cleanup path for dropped-network sessions
     * ({@code PracticeSessionCleanupService}) does not have these two numbers so it returns
     * {@code UNKNOWN} directly instead of guessing -- see the note there.
     *
     * <p>The strength of that signal is decided by {@link #signal} -- see the javadoc there.
     *
     * <p><b>Where the result goes:</b> {@code practice_sessions.abandon_diagnosis}, and the ONLY consumer with any effect
     * is the gate of
     * {@code InterestVectorService.recordSessionOutcome} -- an incomplete session only gets
     * an interest signal recorded when the diagnosis is {@code BORED}. {@code TOO_HARD} deliberately records
     * NOTHING: giving up because it is hard does not mean not liking it.
     */
    public static String diagnose(
            Double normalizedScore,
            int helpRequestCount,
            int longPauseCount) {
        if (normalizedScore == null) {
            return diagnoseWithoutScore(helpRequestCount, longPauseCount);
        }
        if (normalizedScore >= 0.65
                && helpRequestCount == 0
                && longPauseCount <= 1) {
            return "BORED";
        }
        if (normalizedScore < 0.50
                || helpRequestCount >= 1
                || longPauseCount >= 2) {
            return "TOO_HARD";
        }
        return "UNKNOWN";
    }

    /**
     * This is the MAIN path, not a rare exception.
     *
     * <p>{@link #diagnose} is only called when status = ABANDONED, and ABANDONED <=> gradedSeconds == 0
     * <=> the student has not spoken a single turn <=> there is no grading yet <=> normalizedScore ==
     * null. In other words: this branch runs EVERY time, while the two score-based rules in
     * {@code diagnose} never do. Returning UNKNOWN straight away, as before, meant abandon_diagnosis
     * was UNKNOWN forever, BORED never appeared, and since InterestVectorService only records a signal
     * when the session is completed OR BORED, an abandoned session NEVER taught the interest profile
     * anything. Checked on the DB on 2026-08-05: the whole system had 0 BORED sessions and 0 TOO_HARD
     * sessions.
     *
     * <p>There is no score but there IS still behaviour -- and both numbers are measurable even when
     * the student has not opened their mouth, because longPauseCount counts after the AI has finished
     * reading the question.
     *
     * <p>Having tried (asked for a hint, or sat silent for the full 10 seconds across two questions)
     * means TOO_HARD. Leaving before even one full 10-second silence shows no sign of being stuck yet
     * still not wanting to continue; it follows the same reasoning as the high-score rule in
     * {@code diagnose} -- "not struggling yet still quits" -- and means BORED. The false-positive risk
     * (an urgent errand, a mis-tap) is accepted: the signal is only 0.15 and with EMA alpha = 0.3 one
     * wrong call moves the score from 0.50 to 0.395, which recovers within a few sessions. In return
     * this is the ONLY path through which an abandoned session can say anything. Exactly one 10-second
     * silence and then leaving is one data point with two equally likely explanations, so it is not
     * guessed: UNKNOWN.
     */
    private static String diagnoseWithoutScore(int helpRequestCount, int longPauseCount) {
        if (helpRequestCount >= 1 || longPauseCount >= 2) {
            return "TOO_HARD";
        }
        if (longPauseCount == 0) {
            return "BORED";
        }
        return "UNKNOWN";
    }

    /** From this threshold up the signal is full, exactly as before the scale existed. */
    public static final int FULL_SIGNAL_SECONDS = 120;

    /**
     * @param origin where the topic entered the session from: the student searched for it ({@code KEYWORD}), the system probed it
     *               ({@code EXPLORATION}/{@code RANDOM}/{@code EPSILON}), or it was suggested.
     * @param spokenSeconds seconds in which VAD heard speech -- the same number quota deducts, NOT the
     *                      session duration (duration includes when the AI speaks and when the student sits thinking).
     *                      Equals 0 for an abandoned session, in which case the function returns exactly the old level.
     */
    public static double signal(String origin, int spokenSeconds) {
        double abandoned = abandonedSignal(origin);
        double full = fullSignal(origin);
        if (spokenSeconds <= 0) {
            return abandoned;
        }
        if (spokenSeconds >= FULL_SIGNAL_SECONDS) {
            return full;
        }
        double progress = (double) spokenSeconds / FULL_SIGNAL_SECONDS;
        return abandoned + progress * (full - abandoned);
    }

    /**
     * Full level by source -- keeps the old table unchanged.
     *
     * <p>{@code EPSILON} is put in the SAME GROUP as {@code EXPLORATION} (fixed 2026-08-06). Before that it
     * had no case so it fell into {@code default} and got 0.95 -- identical to a student's own choice, exactly the
     * "false positive" that the comment at {@code BuildPracticePaperUseCase.resolveOrigin} thought it had
     * avoided.
     *
     * <p>Why the same group: in both cases the SYSTEM brings the topic, which is not evidence that the
     * student liked it to begin with. For ε-greedy this is also a condition for the measurement to mean something -- if the score
     * of an exploration slot equals that of an exploitation slot, then the very thing exploration exists to measure
     * is contaminated by the advantage of the existing choice.
     *
     * <p>Accepted trade-off: a topic that has only ever entered a session through the exploration path gets at most
     * 0.60 the first time. To go higher the student must ACTIVELY choose it again next time -- exactly what we want to treat
     * as real evidence.
     */
    private static double fullSignal(String origin) {
        return switch (origin == null ? "" : origin) {
            case "KEYWORD" -> 1.00;
            case "EXPLORATION", "RANDOM", "EPSILON" -> 0.60;
            default -> 0.95;
        };
    }

    /**
     * Level for an abandoned session -- also keeps the old table unchanged, and is only reached when
     * {@link SessionDiagnosisPolicy} concludes {@code BORED}.
     *
     * <p>Exploration is penalised LEAST (0.10 versus 0.20 for self-searched) by the same logic as
     * {@link #fullSignal}: when the system brings the topic itself, a student quitting says less about them
     * than when they typed the keyword themselves to search and still quit.
     */
    private static double abandonedSignal(String origin) {
        return switch (origin == null ? "" : origin) {
            case "KEYWORD" -> 0.20;
            case "EXPLORATION", "RANDOM", "EPSILON" -> 0.10;
            default -> 0.15;
        };
    }
}
