package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.FlagExamSessionCommand;
import com.sep.vox.application.port.input.command.ForceEndExamSessionCommand;
import com.sep.vox.application.port.input.command.ReleasePendingExamResultCommand;
import com.sep.vox.application.port.input.command.ReviewFlaggedExamResultCommand;
import com.sep.vox.application.port.input.command.RetryGradingExamSessionCommand;
import com.sep.vox.application.port.input.command.SubmitExamSessionCommand;
import com.sep.vox.application.port.input.command.UnblockExamCandidateCommand;
import com.sep.vox.application.port.input.query.ViewExamItemResponseEvaluationQuery;
import com.sep.vox.application.port.input.query.ViewExamItemResponseQuery;
import com.sep.vox.application.port.input.query.ViewExamItemResponseTurnsQuery;
import com.sep.vox.application.port.input.query.ViewExamSessionFollowupsQuery;
import com.sep.vox.application.port.input.query.ViewExamSessionQuery;
import com.sep.vox.application.port.input.query.ViewExamSessionResultQuery;
import com.sep.vox.application.port.input.usecase.examevaluation.ViewExamItemResponseEvaluationUseCase;
import com.sep.vox.application.port.input.usecase.examcandidate.UnblockExamCandidateUseCase;
import com.sep.vox.application.port.input.usecase.examitemresponse.ViewExamItemResponseTurnsUseCase;
import com.sep.vox.application.port.input.usecase.examitemresponse.ViewExamItemResponseUseCase;
import com.sep.vox.application.port.input.usecase.examitemresponse.ViewExamSessionFollowupsUseCase;
import com.sep.vox.application.port.input.usecase.examsession.FlagExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.ForceEndExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.ReleasePendingExamResultUseCase;
import com.sep.vox.application.port.input.usecase.examsession.ReviewFlaggedExamResultUseCase;
import com.sep.vox.application.port.input.usecase.examsession.RetryGradingExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.SubmitExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.ViewExamSessionResultUseCase;
import com.sep.vox.application.port.input.usecase.examsession.ViewExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.ViewMyExamResultsUseCase;
import com.sep.vox.application.response.input.examitemresponse.ExamItemEvaluationDetailsResponse;
import com.sep.vox.application.response.input.examitemresponse.ExamItemResponseDetailsResponse;
import com.sep.vox.application.response.input.examitemresponse.ExamItemResponseTurnResponse;
import com.sep.vox.application.response.input.examitemresponse.ExamSessionFollowupResponse;
import com.sep.vox.application.response.input.examsession.ExamCandidateResultResponse;
import com.sep.vox.application.response.input.examsession.ExamSessionResponse;
import com.sep.vox.application.response.input.examsession.StudentExamResultSummaryResponse;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;


@Controller("graphqlExamSessionController")
public class ExamSessionController {

    private final ViewExamSessionUseCase viewExamSessionUseCase;
    private final ViewExamSessionResultUseCase viewExamSessionResultUseCase;
    private final ViewExamSessionFollowupsUseCase viewExamSessionFollowupsUseCase;
    private final ViewMyExamResultsUseCase viewMyExamResultsUseCase;
    private final ViewExamItemResponseUseCase viewExamItemResponseUseCase;
    private final ViewExamItemResponseTurnsUseCase viewExamItemResponseTurnsUseCase;
    private final ViewExamItemResponseEvaluationUseCase viewExamItemResponseEvaluationUseCase;
    private final FlagExamSessionUseCase flagExamSessionUseCase;
    private final ForceEndExamSessionUseCase forceEndExamSessionUseCase;
    private final UnblockExamCandidateUseCase unblockExamCandidateUseCase;
    private final ReviewFlaggedExamResultUseCase reviewFlaggedExamResultUseCase;
    private final ReleasePendingExamResultUseCase releasePendingExamResultUseCase;
    private final RetryGradingExamSessionUseCase retryGradingExamSessionUseCase;
    private final SubmitExamSessionUseCase submitExamSessionUseCase;

    public ExamSessionController(
            ViewExamSessionUseCase viewExamSessionUseCase,
            ViewExamSessionResultUseCase viewExamSessionResultUseCase,
            ViewExamSessionFollowupsUseCase viewExamSessionFollowupsUseCase,
            ViewMyExamResultsUseCase viewMyExamResultsUseCase,
            ViewExamItemResponseUseCase viewExamItemResponseUseCase,
            ViewExamItemResponseTurnsUseCase viewExamItemResponseTurnsUseCase,
            ViewExamItemResponseEvaluationUseCase viewExamItemResponseEvaluationUseCase,
            FlagExamSessionUseCase flagExamSessionUseCase,
            ForceEndExamSessionUseCase forceEndExamSessionUseCase,
            UnblockExamCandidateUseCase unblockExamCandidateUseCase,
            ReviewFlaggedExamResultUseCase reviewFlaggedExamResultUseCase,
            ReleasePendingExamResultUseCase releasePendingExamResultUseCase,
            RetryGradingExamSessionUseCase retryGradingExamSessionUseCase,
            SubmitExamSessionUseCase submitExamSessionUseCase) {
        this.viewExamSessionUseCase = viewExamSessionUseCase;
        this.viewExamSessionResultUseCase = viewExamSessionResultUseCase;
        this.viewExamSessionFollowupsUseCase = viewExamSessionFollowupsUseCase;
        this.viewMyExamResultsUseCase = viewMyExamResultsUseCase;
        this.viewExamItemResponseUseCase = viewExamItemResponseUseCase;
        this.viewExamItemResponseTurnsUseCase = viewExamItemResponseTurnsUseCase;
        this.viewExamItemResponseEvaluationUseCase = viewExamItemResponseEvaluationUseCase;
        this.flagExamSessionUseCase = flagExamSessionUseCase;
        this.forceEndExamSessionUseCase = forceEndExamSessionUseCase;
        this.unblockExamCandidateUseCase = unblockExamCandidateUseCase;
        this.reviewFlaggedExamResultUseCase = reviewFlaggedExamResultUseCase;
        this.releasePendingExamResultUseCase = releasePendingExamResultUseCase;
        this.retryGradingExamSessionUseCase = retryGradingExamSessionUseCase;
        this.submitExamSessionUseCase = submitExamSessionUseCase;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ExamSessionResponse examSession(@Argument(name = "id") UUID id) {
        return viewExamSessionUseCase.execute(new ViewExamSessionQuery(id));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ExamCandidateResultResponse examSessionResult(@Argument(name = "sessionId") UUID sessionId) {
        try {
            return viewExamSessionResultUseCase.execute(new ViewExamSessionResultQuery(sessionId));
        } catch (NotFoundException ex) {
            return null;
        }
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public List<ExamSessionFollowupResponse> examSessionFollowups(@Argument(name = "sessionId") UUID sessionId) {
        return viewExamSessionFollowupsUseCase.execute(new ViewExamSessionFollowupsQuery(sessionId));
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<StudentExamResultSummaryResponse> myExamResults() {
        return viewMyExamResultsUseCase.execute(null);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ExamItemResponseDetailsResponse examItemResponse(@Argument(name = "answerId") UUID answerId) {
        return viewExamItemResponseUseCase.execute(new ViewExamItemResponseQuery(answerId));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public List<ExamItemResponseTurnResponse> examItemResponseTurns(@Argument(name = "answerId") UUID answerId) {
        return viewExamItemResponseTurnsUseCase.execute(new ViewExamItemResponseTurnsQuery(answerId));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ExamItemEvaluationDetailsResponse examItemResponseEvaluation(@Argument(name = "answerId") UUID answerId) {
        try {
            return viewExamItemResponseEvaluationUseCase.execute(new ViewExamItemResponseEvaluationQuery(answerId));
        } catch (NotFoundException ex) {
            return null;
        }
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID flagExamSession(
            @Argument(name = "sessionId") UUID sessionId,
            @Argument(name = "reason") String reason) {
        return flagExamSessionUseCase.execute(new FlagExamSessionCommand(sessionId, reason));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID forceEndExamSession(
            @Argument(name = "sessionId") UUID sessionId,
            @Argument(name = "reason") String reason) {
        return forceEndExamSessionUseCase.execute(new ForceEndExamSessionCommand(sessionId, reason));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID unblockExamCandidate(
            @Argument(name = "candidateId") UUID candidateId,
            @Argument(name = "reason") String reason) {
        return unblockExamCandidateUseCase.execute(new UnblockExamCandidateCommand(candidateId, reason)).id();
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID reviewFlaggedExamResult(
            @Argument(name = "candidateResultId") UUID candidateResultId,
            @Argument(name = "decision") ExamCandidateResultStatus decision) {
        return reviewFlaggedExamResultUseCase.execute(new ReviewFlaggedExamResultCommand(candidateResultId, decision));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID releasePendingExamResult(@Argument(name = "sessionId") UUID sessionId) {
        return releasePendingExamResultUseCase.execute(new ReleasePendingExamResultCommand(sessionId));
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID retryGradingExamSession(@Argument(name = "sessionId") UUID sessionId) {
        return retryGradingExamSessionUseCase.execute(new RetryGradingExamSessionCommand(sessionId));
    }

    // TEST-ONLY: chấm lại bằng AI từ đầu bất kể trạng thái (kể cả GRADED). SubmitExamSessionUseCase
    // đã chấp nhận GRADED và re-publish grading request từ dữ liệu làm bài đã lưu; evaluation cũ
    // bị RecordExamAttemptEvaluationUseCase ghi đè (upsert). Không xoá tay gì.
    @MutationMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public UUID regradeExamSessionForTest(@Argument(name = "sessionId") UUID sessionId) {
        submitExamSessionUseCase.execute(new SubmitExamSessionCommand(sessionId));
        return sessionId;
    }

}
