package com.sep.vox.domain.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentProctoringAlert;

public interface AssessmentProctoringAlertRepository {

    /**
     * Records an alert if {@code eventId} has never been recorded, and reports whether a new row was written.
     *
     * <p>Returns a boolean rather than void because the caller must tell "saved" apart from "this is a resend":
     * both succeed, but only one is new data worth logging.
     */
    boolean saveIfAbsent(AssessmentProctoringAlert alert);

    List<AssessmentProctoringAlert> findByAssessmentSessionIdOrderByCapturedAt(UUID assessmentSessionId);

    /**
     * Does this session have any alert belonging to {@link AssessmentProctoringAlert#INTEGRITY_ALERT_TYPES} --
     * i.e. a reason to suspect the attempt was not done by the candidate themself.
     *
     * <p>Used when writing the grading result to push the attempt to waiting for a reviewer -- see
     * {@code RecordAssessmentAttemptEvaluationUseCase}. Returns a boolean and not a list because the caller
     * only needs to know whether there is one.
     *
     * <p>Asked by TYPE and not by {@code level} -- the full reason is in the javadoc of
     * {@code INTEGRITY_ALERT_TYPES}. In short: level is the scale of "what must the proctor do RIGHT NOW", this gate
     * asks "is the result trustworthy", and one adjustment of that scale has silently left this gate open.
     */
    boolean hasIntegrityAlert(UUID assessmentSessionId);

    /** Alerts of a whole schedule, merged across every session in it -- for the live monitoring screen. */
    List<AssessmentProctoringAlert> findByScheduleIdOrderByCapturedAt(UUID scheduleId);
}
