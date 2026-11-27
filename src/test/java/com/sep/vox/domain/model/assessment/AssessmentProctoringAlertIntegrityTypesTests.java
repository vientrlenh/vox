package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Locks the LIST of alert types that force an attempt to go through a reviewer.
 *
 * <p>Born from a real incident. This gate used to ask {@code level = 'CRITICAL'}, and when vox-streaming
 * lowered MULTIPLE_PERSONS from CRITICAL to WARNING ({@code 4cc6598}), an assessment session with a second person
 * sitting together silently stopped being sent to a reviewer. No test went red: the tests of
 * {@code RecordAssessmentAttemptEvaluationUseCase} mock the repository directly, so they lock "if there is an alert then
 * it must be reviewed" without locking "which alerts count".
 *
 * <p>This is the place that locks the missing half. It deliberately does NOT derive the list from anywhere -- every
 * name is written out, because its purpose is to force whoever edits to change two places and see the decision they are
 * changing.
 */
class AssessmentProctoringAlertIntegrityTypesTests {

    /**
     * MULTIPLE_PERSONS is a case that is already broken, so it stands on its own: its level is WARNING yet it must still be in
     * this group. That apparent paradox is exactly what this rule asserts -- "the proctor does not need to run over
     * right away" and "the result is no longer trustworthy" are two different things.
     */
    @Test
    void a_second_person_in_frame_forces_review_even_though_its_alert_level_is_only_warning() {
        assertThat(AssessmentProctoringAlert.INTEGRITY_ALERT_TYPES).contains("MULTIPLE_PERSONS");
    }

    @Test
    void the_integrity_group_is_exactly_the_three_types_that_question_who_answered() {
        assertThat(AssessmentProctoringAlert.INTEGRITY_ALERT_TYPES)
            .containsExactlyInAnyOrder("MULTIPLE_PERSONS", "PHONE_DETECTED", "PROHIBITED_OBJECT");
    }

    /**
     * The other half of the decision, and the half that keeps the review meaningful: the types that happen frequently
     * in a real assessment must NOT get in. A session with 16 PERSON_MISSING turns is a real measurement; counting them
     * would send almost every result to awaiting review.
     */
    @Test
    void routine_assessment_noise_stays_out_so_that_review_keeps_meaning_something() {
        assertThat(AssessmentProctoringAlert.INTEGRITY_ALERT_TYPES)
            .doesNotContain(
                "PERSON_MISSING",
                "WINDOW_FOCUS_LOST",
                "CAMERA_SIGNAL_LOST",
                "CAMERA_SIGNAL_RESTORED",
                "STREAM_DROPPED",
                "RECORDING_INCOMPLETE",
                "RECORDING_TRUNCATED",
                "UNCOOPERATIVE_CANDIDATE");
    }

    /**
     * The list is compared with {@code UPPER(TRIM(...))} in JPQL, so a lowercase name here would never match any record
     * -- and it fails in the quietest way possible: the review gate still runs, it just catches nothing.
     */
    @Test
    void every_type_is_stored_upper_case_because_the_query_compares_upper_case() {
        assertThat(AssessmentProctoringAlert.INTEGRITY_ALERT_TYPES)
            .allSatisfy(type -> assertThat(type).isEqualTo(type.toUpperCase().trim()));
    }
}
