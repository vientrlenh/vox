package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.usecase.recording.GetAssessmentRecordingPlaybackUseCase;
import com.sep.vox.application.port.input.usecase.recording.GetAssessmentRecordsUseCase;
import com.sep.vox.application.response.input.recording.AssessmentRecordingPlaybackResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.DecideAssessmentCandidateResultOutcomeCommand;
import com.sep.vox.application.port.input.command.FlagAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.ForceEndAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.RetryGradingAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.SubmitAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.UnblockAssessmentCandidateCommand;
import com.sep.vox.application.port.input.query.GetAssessmentRecordsQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentItemResponseEvaluationQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentItemResponseQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentItemResponseTurnsQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentSessionFollowupsQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentSessionQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentSessionResultQuery;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.ViewAssessmentItemResponseEvaluationUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.UnblockAssessmentCandidateUseCase;
import com.sep.vox.application.port.input.usecase.assessmentitemresponse.ViewAssessmentItemResponseTurnsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentitemresponse.ViewAssessmentItemResponseUseCase;
import com.sep.vox.application.port.input.usecase.assessmentitemresponse.ViewAssessmentSessionFollowupsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.DecideAssessmentCandidateResultOutcomeUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.FlagAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.ForceEndAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.RetryGradingAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.SubmitAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.ViewAssessmentSessionResultUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.ViewAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.ViewMyAssessmentResultsUseCase;
import com.sep.vox.application.port.input.usecase.proctoring.ViewAssessmentSessionProctoringAlertsUseCase;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemEvaluationDetailsResponse;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemResponseDetailsResponse;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemResponseTurnResponse;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentSessionFollowupResponse;
import com.sep.vox.application.response.input.assessmentsession.AssessmentCandidateResultResponse;
import com.sep.vox.application.response.input.assessmentsession.AssessmentSessionResponse;
import com.sep.vox.application.response.input.assessmentsession.StudentAssessmentResultSummaryResponse;
import com.sep.vox.domain.dto.AssessmentProctoringAlertDto;
import com.sep.vox.domain.dto.AssessmentRecordingDto;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;


@Controller("graphqlAssessmentSessionController")
@RequiredArgsConstructor
public class AssessmentSessionController {

    private final ViewAssessmentSessionUseCase viewAssessmentSessionUseCase;
    private final ViewAssessmentSessionResultUseCase viewAssessmentSessionResultUseCase;
    private final ViewAssessmentSessionFollowupsUseCase viewAssessmentSessionFollowupsUseCase;
    private final ViewMyAssessmentResultsUseCase viewMyAssessmentResultsUseCase;
    private final ViewAssessmentItemResponseUseCase viewAssessmentItemResponseUseCase;
    private final ViewAssessmentItemResponseTurnsUseCase viewAssessmentItemResponseTurnsUseCase;
    private final ViewAssessmentItemResponseEvaluationUseCase viewAssessmentItemResponseEvaluationUseCase;
    private final FlagAssessmentSessionUseCase flagAssessmentSessionUseCase;
    private final ForceEndAssessmentSessionUseCase forceEndAssessmentSessionUseCase;
    private final UnblockAssessmentCandidateUseCase unblockAssessmentCandidateUseCase;
    private final DecideAssessmentCandidateResultOutcomeUseCase decideAssessmentCandidateResultOutcomeUseCase;
    private final RetryGradingAssessmentSessionUseCase retryGradingAssessmentSessionUseCase;
    private final SubmitAssessmentSessionUseCase submitAssessmentSessionUseCase;
    private final GetAssessmentRecordsUseCase getAssessmentRecordsUseCase;
    private final GetAssessmentRecordingPlaybackUseCase getAssessmentRecordingPlaybackUseCase;
    private final ViewAssessmentSessionProctoringAlertsUseCase viewAssessmentSessionProctoringAlertsUseCase;


    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public List<AssessmentRecordingDto> recordings(@Argument(name = "assessmentSessionId") UUID assessmentSessionId, @Argument(name = "streamType") String streamType) {
        GetAssessmentRecordsQuery query = new GetAssessmentRecordsQuery(assessmentSessionId, streamType);
        return getAssessmentRecordsUseCase.execute(query);
    }

    /**
     * The recording with a playback link, for the grading screen. Kept apart from {@link #recordings} because that one is the API
     * of the proctoring app and does not need a link -- see the note in GetAssessmentRecordingPlaybackUseCase.
     */
    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public List<AssessmentRecordingPlaybackResponse> assessmentRecordingPlayback(
            @Argument(name = "assessmentSessionId") UUID assessmentSessionId) {
        return getAssessmentRecordingPlaybackUseCase.execute(new GetAssessmentRecordsQuery(assessmentSessionId, null));
    }

    /**
     * The proctoring alerts of an assessment session, in chronological order.
     *
     * <p>The same permission rule as {@link #assessmentRecordingPlayback}: these are the same kind of evidence
     * about the same person, so they must be open to the same set of people.
     */
    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public List<AssessmentProctoringAlertDto> assessmentSessionProctoringAlerts(
            @Argument(name = "assessmentSessionId") UUID assessmentSessionId) {
        return viewAssessmentSessionProctoringAlertsUseCase.execute(assessmentSessionId);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public AssessmentSessionResponse assessmentSession(@Argument(name = "id") UUID id) {
        return viewAssessmentSessionUseCase.execute(new ViewAssessmentSessionQuery(id));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public AssessmentCandidateResultResponse assessmentSessionResult(@Argument(name = "sessionId") UUID sessionId) {
        try {
            return viewAssessmentSessionResultUseCase.execute(new ViewAssessmentSessionResultQuery(sessionId));
        } catch (ResourceNotFoundException ex) {
            return null;
        }
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public List<AssessmentSessionFollowupResponse> assessmentSessionFollowups(@Argument(name = "sessionId") UUID sessionId) {
        return viewAssessmentSessionFollowupsUseCase.execute(new ViewAssessmentSessionFollowupsQuery(sessionId));
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<StudentAssessmentResultSummaryResponse> myAssessmentResults() {
        return viewMyAssessmentResultsUseCase.execute(null);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public AssessmentItemResponseDetailsResponse assessmentItemResponse(@Argument(name = "answerId") UUID answerId) {
        return viewAssessmentItemResponseUseCase.execute(new ViewAssessmentItemResponseQuery(answerId));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public List<AssessmentItemResponseTurnResponse> assessmentItemResponseTurns(@Argument(name = "answerId") UUID answerId) {
        return viewAssessmentItemResponseTurnsUseCase.execute(new ViewAssessmentItemResponseTurnsQuery(answerId));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public AssessmentItemEvaluationDetailsResponse assessmentItemResponseEvaluation(@Argument(name = "answerId") UUID answerId) {
        try {
            return viewAssessmentItemResponseEvaluationUseCase.execute(new ViewAssessmentItemResponseEvaluationQuery(answerId));
        } catch (ResourceNotFoundException ex) {
            return null;
        }
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID flagAssessmentSession(
            @Argument(name = "sessionId") UUID sessionId,
            @Argument(name = "flagged") boolean flagged,
            @Argument(name = "reason") String reason) {
        return flagAssessmentSessionUseCase.execute(new FlagAssessmentSessionCommand(sessionId, flagged, reason));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID forceEndAssessmentSession(
            @Argument(name = "sessionId") UUID sessionId,
            @Argument(name = "reason") String reason) {
        return forceEndAssessmentSessionUseCase.execute(new ForceEndAssessmentSessionCommand(sessionId, reason));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID unblockAssessmentCandidate(
            @Argument(name = "candidateId") UUID candidateId,
            @Argument(name = "reason") String reason) {
        return unblockAssessmentCandidateUseCase.execute(new UnblockAssessmentCandidateCommand(candidateId, reason)).id();
    }

    // reviewFlaggedAssessmentResult / releasePendingAssessmentResult were removed from admin permission:
    // the decider is THE TEACHER HOLDING THE ATTEMPT, through /uphold, /regrade, /invalidate of
    // GradingAssignmentController. An admin re-reviewing every attempt is a bottleneck that
    // creates no value -- see AI/claude/plans.md 2026-07-26.

    @MutationMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID decideAssessmentCandidateResultOutcome(
            @Argument(name = "candidateResultId") UUID candidateResultId,
            @Argument(name = "decision") AssessmentCandidateResultStatus decision) {
        return decideAssessmentCandidateResultOutcomeUseCase.execute(
            new DecideAssessmentCandidateResultOutcomeCommand(candidateResultId, decision)
        );
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID retryGradingAssessmentSession(@Argument(name = "sessionId") UUID sessionId) {
        return retryGradingAssessmentSessionUseCase.execute(new RetryGradingAssessmentSessionCommand(sessionId));
    }

    // TEST-ONLY: regrades with the AI from scratch regardless of status (including GRADED). SubmitAssessmentSessionUseCase
    // already accepts GRADED and re-publishes the grading request from the stored attempt data; the old evaluation
    // is overwritten (upsert) by RecordAssessmentAttemptEvaluationUseCase. Nothing is deleted by hand.
    @MutationMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID regradeAssessmentSessionForTest(@Argument(name = "sessionId") UUID sessionId) {
        submitAssessmentSessionUseCase.execute(new SubmitAssessmentSessionCommand(sessionId));
        return sessionId;
    }

}
