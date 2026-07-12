package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewExamItemResponseEvaluationQuery;
import com.sep.vox.application.port.input.query.ViewExamItemResponseQuery;
import com.sep.vox.application.port.input.query.ViewExamItemResponseTurnsQuery;
import com.sep.vox.application.port.input.query.ViewExamSessionFollowupsQuery;
import com.sep.vox.application.port.input.query.ViewExamSessionQuery;
import com.sep.vox.application.port.input.query.ViewExamSessionResultQuery;
import com.sep.vox.application.port.input.usecase.examevaluation.ViewExamItemResponseEvaluationUseCase;
import com.sep.vox.application.port.input.usecase.examitemresponse.ViewExamItemResponseTurnsUseCase;
import com.sep.vox.application.port.input.usecase.examitemresponse.ViewExamItemResponseUseCase;
import com.sep.vox.application.port.input.usecase.examitemresponse.ViewExamSessionFollowupsUseCase;
import com.sep.vox.application.port.input.usecase.examsession.ViewExamSessionResultUseCase;
import com.sep.vox.application.port.input.usecase.examsession.ViewExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.ViewMyExamResultsUseCase;
import com.sep.vox.application.response.input.examitemresponse.ExamItemCriterionScoreResponse;
import com.sep.vox.application.response.input.examitemresponse.ExamItemEvaluationDetailsResponse;
import com.sep.vox.application.response.input.examitemresponse.ExamItemEvaluationTurnResponse;
import com.sep.vox.application.response.input.examitemresponse.ExamItemResponseDetailsResponse;
import com.sep.vox.application.response.input.examitemresponse.ExamItemResponseTurnResponse;
import com.sep.vox.application.response.input.examitemresponse.ExamSessionFollowupResponse;
import com.sep.vox.application.response.input.examsession.ExamCandidateResultItemResponse;
import com.sep.vox.application.response.input.examsession.ExamCandidateResultResponse;
import com.sep.vox.application.response.input.examsession.ExamCandidateResultSectionResponse;
import com.sep.vox.application.response.input.examsession.ExamSessionResponse;
import com.sep.vox.application.response.input.examsession.StudentExamResultSummaryResponse;

import tools.jackson.databind.JsonNode;

@Controller("graphqlExamSessionController")
public class ExamSessionController {

    private final ViewExamSessionUseCase viewExamSessionUseCase;
    private final ViewExamSessionResultUseCase viewExamSessionResultUseCase;
    private final ViewExamSessionFollowupsUseCase viewExamSessionFollowupsUseCase;
    private final ViewMyExamResultsUseCase viewMyExamResultsUseCase;
    private final ViewExamItemResponseUseCase viewExamItemResponseUseCase;
    private final ViewExamItemResponseTurnsUseCase viewExamItemResponseTurnsUseCase;
    private final ViewExamItemResponseEvaluationUseCase viewExamItemResponseEvaluationUseCase;

    public ExamSessionController(
            ViewExamSessionUseCase viewExamSessionUseCase,
            ViewExamSessionResultUseCase viewExamSessionResultUseCase,
            ViewExamSessionFollowupsUseCase viewExamSessionFollowupsUseCase,
            ViewMyExamResultsUseCase viewMyExamResultsUseCase,
            ViewExamItemResponseUseCase viewExamItemResponseUseCase,
            ViewExamItemResponseTurnsUseCase viewExamItemResponseTurnsUseCase,
            ViewExamItemResponseEvaluationUseCase viewExamItemResponseEvaluationUseCase) {
        this.viewExamSessionUseCase = viewExamSessionUseCase;
        this.viewExamSessionResultUseCase = viewExamSessionResultUseCase;
        this.viewExamSessionFollowupsUseCase = viewExamSessionFollowupsUseCase;
        this.viewMyExamResultsUseCase = viewMyExamResultsUseCase;
        this.viewExamItemResponseUseCase = viewExamItemResponseUseCase;
        this.viewExamItemResponseTurnsUseCase = viewExamItemResponseTurnsUseCase;
        this.viewExamItemResponseEvaluationUseCase = viewExamItemResponseEvaluationUseCase;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ExamSessionResponse examSession(@Argument UUID id) {
        return viewExamSessionUseCase.execute(new ViewExamSessionQuery(id));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ExamCandidateResultResponse examSessionResult(@Argument("sessionId") UUID sessionId) {
        try {
            return viewExamSessionResultUseCase.execute(new ViewExamSessionResultQuery(sessionId));
        } catch (NotFoundException ex) {
            return null;
        }
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public List<ExamSessionFollowupResponse> examSessionFollowups(@Argument("sessionId") UUID sessionId) {
        return viewExamSessionFollowupsUseCase.execute(new ViewExamSessionFollowupsQuery(sessionId));
    }

    @QueryMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<StudentExamResultSummaryResponse> myExamResults() {
        return viewMyExamResultsUseCase.execute(null);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ExamItemResponseDetailsResponse examItemResponse(@Argument UUID answerId) {
        return viewExamItemResponseUseCase.execute(new ViewExamItemResponseQuery(answerId));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public List<ExamItemResponseTurnResponse> examItemResponseTurns(@Argument UUID answerId) {
        return viewExamItemResponseTurnsUseCase.execute(new ViewExamItemResponseTurnsQuery(answerId));
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'STUDENT')")
    public ExamItemEvaluationDetailsGraphQlResponse examItemResponseEvaluation(@Argument UUID answerId) {
        try {
            var response = viewExamItemResponseEvaluationUseCase.execute(new ViewExamItemResponseEvaluationQuery(answerId));
            return response == null ? null : toGraphQlResponse(response);
        } catch (NotFoundException ex) {
            return null;
        }
    }

    private ExamItemEvaluationDetailsGraphQlResponse toGraphQlResponse(ExamItemEvaluationDetailsResponse response) {
        return new ExamItemEvaluationDetailsGraphQlResponse(
            response.id(),
            response.responseId(),
            response.paperItemId(),
            response.engineType(),
            response.gradedByModel(),
            response.promptVersion(),
            response.rawItemScore(),
            response.itemScore(),
            response.overallConfidence(),
            response.markedInvalid(),
            response.requiresRetake(),
            response.status(),
            response.evaluatedAt(),
            response.feedbackSummary(),
            stringify(response.signals()),
            stringify(response.validity()),
            stringify(response.suggestions()),
            response.criteria(),
            response.turns().stream()
                .map(turn -> new ExamItemEvaluationTurnGraphQlResponse(
                    turn.id(),
                    turn.turnOrder(),
                    turn.turnType(),
                    turn.promptText(),
                    turn.audioUrl(),
                    turn.transcript(),
                    turn.wordCount(),
                    turn.durationSeconds(),
                    turn.asrConfidence(),
                    stringify(turn.pronunciationOverall()),
                    stringify(turn.wordFeedback())
                ))
                .toList()
        );
    }

    private String stringify(JsonNode node) {
        return node == null ? null : node.toString();
    }

    private record ExamItemEvaluationTurnGraphQlResponse(
        UUID id,
        Integer turnOrder,
        String turnType,
        String promptText,
        String audioUrl,
        String transcript,
        Integer wordCount,
        Integer durationSeconds,
        Double asrConfidence,
        String pronunciationOverall,
        String wordFeedback
    ) {
    }

    private record ExamItemEvaluationDetailsGraphQlResponse(
        UUID id,
        UUID responseId,
        UUID paperItemId,
        String engineType,
        String gradedByModel,
        String promptVersion,
        java.math.BigDecimal rawItemScore,
        java.math.BigDecimal itemScore,
        java.math.BigDecimal overallConfidence,
        boolean markedInvalid,
        boolean requiresRetake,
        String status,
        String evaluatedAt,
        String feedbackSummary,
        String signals,
        String validity,
        String suggestions,
        List<ExamItemCriterionScoreResponse> criteria,
        List<ExamItemEvaluationTurnGraphQlResponse> turns
    ) {
    }
}
