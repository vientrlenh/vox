package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.usecase.recording.GetAssessmentRecordingPlaybackUseCase;
import com.sep.vox.application.port.input.usecase.recording.GetAssessmentRecordsUseCase;
import com.sep.vox.application.response.input.recording.AssessmentRecordingPlaybackResponse;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.command.DecideAssessmentCandidateResultOutcomeCommand;
import com.sep.vox.application.port.input.command.FlagAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.ForceEndAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.RetryGradingAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.SubmitAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.UnblockAssessmentCandidateCommand;
import com.sep.vox.application.port.input.query.GetAssessmentRecordsQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentItemAnswerEvaluationQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentItemAnswerQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentItemAnswerTurnsQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentSessionFollowupsQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentSessionQuery;
import com.sep.vox.application.port.input.query.ViewAssessmentSessionResultQuery;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.UnblockAssessmentCandidateUseCase;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.ViewAssessmentItemAnswerEvaluationUseCase;
import com.sep.vox.application.port.input.usecase.assessmentitemanswer.ViewAssessmentItemAnswerTurnsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentitemanswer.ViewAssessmentItemAnswerUseCase;
import com.sep.vox.application.port.input.usecase.assessmentitemanswer.ViewAssessmentSessionFollowupsUseCase;
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
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemAnswerDetailsResponse;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemAnswerTurnResponse;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentSessionFollowupResponse;
import com.sep.vox.application.response.input.assessmentsession.AssessmentCandidateResultResponse;
import com.sep.vox.application.response.input.assessmentsession.AssessmentSessionResponse;
import com.sep.vox.application.response.input.assessmentsession.StudentAssessmentResultSummaryResponse;
import com.sep.vox.domain.dto.assessment.AssessmentProctoringAlertDto;
import com.sep.vox.domain.dto.assessment.AssessmentRecordingDto;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor 
public class GraphQLAssessmentSessionController {

    private final ViewAssessmentSessionUseCase viewAssessmentSessionUseCase;
    private final ViewAssessmentSessionResultUseCase viewAssessmentSessionResultUseCase;
    private final ViewAssessmentSessionFollowupsUseCase viewAssessmentSessionFollowupsUseCase;
    private final ViewMyAssessmentResultsUseCase viewMyAssessmentResultsUseCase;
    private final ViewAssessmentItemAnswerUseCase viewAssessmentItemAnswerUseCase;
    private final ViewAssessmentItemAnswerTurnsUseCase viewAssessmentItemAnswerTurnsUseCase;
    private final ViewAssessmentItemAnswerEvaluationUseCase viewAssessmentItemAnswerEvaluationUseCase;
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
        var query = new GetAssessmentRecordsQuery(assessmentSessionId, streamType);
        return getAssessmentRecordsUseCase.execute(query);
    }

    /**
     * Bản ghi kèm link phát, cho màn chấm bài. Tách khỏi {@link #recordings} vì đường kia là API
     * của app giám thị và không cần link -- xem chú thích ở GetAssessmentRecordingPlaybackUseCase.
     */
    @QueryMapping(name = "assessmentRecordingPlayback")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public List<AssessmentRecordingPlaybackResponse> assessmentRecordingPlayback(
            @Argument(name = "assessmentSessionId") UUID assessmentSessionId) {
        return getAssessmentRecordingPlaybackUseCase.execute(new GetAssessmentRecordsQuery(assessmentSessionId, null));
    }

    /**
     * Cảnh báo giám sát của một phiên thi, theo thứ tự thời gian.
     *
     * <p>Cùng luật quyền với {@link #assessmentRecordingPlayback}: hai thứ này là cùng một loại bằng chứng
     * về cùng một con người, nên chúng phải mở cho cùng một tập người.
     */
    @QueryMapping(name = "assessmentSessionProctoringAlerts")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public List<AssessmentProctoringAlertDto> assessmentSessionProctoringAlerts(
            @Argument(name = "assessmentSessionId") UUID assessmentSessionId) {
        return viewAssessmentSessionProctoringAlertsUseCase.execute(assessmentSessionId);
    }

    @QueryMapping(name = "assessmentSession")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public AssessmentSessionResponse assessmentSession(@Argument(name = "id") UUID id) {
        return viewAssessmentSessionUseCase.execute(new ViewAssessmentSessionQuery(id));
    }

    @QueryMapping(name = "assessmentSessionResult")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public AssessmentCandidateResultResponse assessmentSessionResult(@Argument(name = "sessionId") UUID sessionId) {
        try {
            return viewAssessmentSessionResultUseCase.execute(new ViewAssessmentSessionResultQuery(sessionId));
        } catch (ResourceNotFoundException ex) {
            return null;
        }
    }

    @QueryMapping(name = "assessmentSessionFollowups")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public List<AssessmentSessionFollowupResponse> assessmentSessionFollowups(@Argument(name = "sessionId") UUID sessionId) {
        return viewAssessmentSessionFollowupsUseCase.execute(new ViewAssessmentSessionFollowupsQuery(sessionId));
    }

    @QueryMapping(name = "myAssessmentResults")
    @PreAuthorize("hasRole('STUDENT')")
    public List<StudentAssessmentResultSummaryResponse> myAssessmentResults() {
        return viewMyAssessmentResultsUseCase.execute(null);
    }

    @QueryMapping(name = "assessmentItemAnswer")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public AssessmentItemAnswerDetailsResponse assessmentItemAnswer(@Argument(name = "answerId") UUID answerId) {
        return viewAssessmentItemAnswerUseCase.execute(new ViewAssessmentItemAnswerQuery(answerId));
    }

    @QueryMapping(name = "assessmentItemAnswerTurns")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public List<AssessmentItemAnswerTurnResponse> assessmentItemAnswerTurns(@Argument(name = "answerId") UUID answerId) {
        return viewAssessmentItemAnswerTurnsUseCase.execute(new ViewAssessmentItemAnswerTurnsQuery(answerId));
    }

    @QueryMapping(name = "assessmentItemAnswerEvaluation")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public AssessmentItemEvaluationDetailsResponse assessmentItemAnswerEvaluation(@Argument(name = "answerId") UUID answerId) {
        try {
            return viewAssessmentItemAnswerEvaluationUseCase.execute(new ViewAssessmentItemAnswerEvaluationQuery(answerId));
        } catch (ResourceNotFoundException ex) {
            return null;
        }
    }

    @MutationMapping(name = "flagAssessmentSession")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID flagAssessmentSession(
            @Argument(name = "sessionId") UUID sessionId,
            @Argument(name = "flagged") boolean flagged,
            @Argument(name = "reason") String reason) {
        return flagAssessmentSessionUseCase.execute(new FlagAssessmentSessionCommand(sessionId, flagged, reason));
    }

    @MutationMapping(name = "forceEndAssessmentSession")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID forceEndAssessmentSession(
            @Argument(name = "sessionId") UUID sessionId,
            @Argument(name = "reason") String reason) {
        return forceEndAssessmentSessionUseCase.execute(new ForceEndAssessmentSessionCommand(sessionId, reason));
    }

    @MutationMapping(name = "unblockAssessmentCandidate")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID unblockAssessmentCandidate(
            @Argument(name = "candidateId") UUID candidateId,
            @Argument(name = "reason") String reason) {
        return unblockAssessmentCandidateUseCase.execute(new UnblockAssessmentCandidateCommand(candidateId, reason)).id();
    }

    // reviewFlaggedAssessmentResult / releasePendingAssessmentResult đã bị gỡ khỏi quyền admin:
    // người quyết là GIÁO VIÊN ĐANG CẦM BÀI, qua /uphold, /regrade, /invalidate của
    // GradingAssignmentController. Admin đi duyệt lại từng bài là nút cổ chai không
    // tạo giá trị — xem AI/claude/plans.md 2026-07-26.

    @MutationMapping(name = "decideAssessmentCandidateResultOutcome")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID decideAssessmentCandidateResultOutcome(
            @Argument(name = "candidateResultId") UUID candidateResultId,
            @Argument(name = "decision") AssessmentCandidateResultStatus decision) {
        return decideAssessmentCandidateResultOutcomeUseCase.execute(
            new DecideAssessmentCandidateResultOutcomeCommand(candidateResultId, decision)
        );
    }

    @MutationMapping(name = "retryGradingAssessmentSession")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID retryGradingAssessmentSession(@Argument(name = "sessionId") UUID sessionId) {
        return retryGradingAssessmentSessionUseCase.execute(new RetryGradingAssessmentSessionCommand(sessionId));
    }

    // TEST-ONLY: chấm lại bằng AI từ đầu bất kể trạng thái (kể cả GRADED). SubmitAssessmentSessionUseCase
    // đã chấp nhận GRADED và re-publish grading request từ dữ liệu làm bài đã lưu; evaluation cũ
    // bị RecordAssessmentAttemptEvaluationUseCase ghi đè (upsert). Không xoá tay gì.
    @MutationMapping(name = "regradeAssessmentSessionForTest")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID regradeAssessmentSessionForTest(@Argument(name = "sessionId") UUID sessionId) {
        submitAssessmentSessionUseCase.execute(new SubmitAssessmentSessionCommand(sessionId));
        return sessionId;
    }

}
