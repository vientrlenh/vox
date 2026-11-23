package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.query.recording.GetAssessmentRecordsQuery;
import com.sep.vox.application.response.input.recording.AssessmentRecordingPlaybackResponse;
import com.sep.vox.domain.dto.AssessmentProctoringAlertDto;
import com.sep.vox.domain.dto.AssessmentRecordingDto;
import com.sep.vox.domain.model.assessment.AssessmentRecordingAssemblyStatus;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.UnblockAssessmentCandidateUseCase;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.ViewAssessmentItemResponseEvaluationUseCase;
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
import com.sep.vox.application.port.input.usecase.recording.GetAssessmentRecordingPlaybackUseCase;
import com.sep.vox.application.port.input.usecase.recording.GetAssessmentRecordsUseCase;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemAiEvaluationContextResponse;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemEvaluationDetailsResponse;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemEvaluationTurnResponse;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemResponseDetailsResponse;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemResponseTurnResponse;
import com.sep.vox.application.response.input.assessmentsession.AssessmentSessionResponse;
import com.sep.vox.application.response.input.assessmentsession.StudentAssessmentResultSummaryResponse;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentEvaluationEngineType;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationStatus;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.TurnType;
import com.sep.vox.support.GraphQLControllerTest;

/**
 * Sends session, answer, evaluation and result queries through the real schema and asserts the typed values: the
 * {@code DateTime} scalar and the {@code AssessmentSessionStatus}, {@code TurnType}, {@code AssessmentEvaluationEngineType},
 * {@code AssessmentItemEvaluationStatus} and {@code AssessmentCandidateResultStatus} enums.
 */
@GraphQLControllerTest(GraphQLAssessmentSessionController.class)
@MockitoBean(types = {
    ViewAssessmentSessionResultUseCase.class, ViewAssessmentSessionFollowupsUseCase.class,
    ViewAssessmentItemResponseTurnsUseCase.class, FlagAssessmentSessionUseCase.class,
    ForceEndAssessmentSessionUseCase.class, UnblockAssessmentCandidateUseCase.class,
    DecideAssessmentCandidateResultOutcomeUseCase.class, RetryGradingAssessmentSessionUseCase.class,
    SubmitAssessmentSessionUseCase.class })
class AssessmentSessionGraphQLTests {

    private static final UUID ID = UUID.randomUUID();
    private static final Instant STARTED_AT = Instant.parse("2026-08-01T01:00:00Z");
    private static final Instant SUBMITTED_AT = Instant.parse("2026-08-01T02:15:30Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewAssessmentSessionUseCase viewAssessmentSessionUseCase;
    @MockitoBean
    private ViewMyAssessmentResultsUseCase viewMyAssessmentResultsUseCase;
    @MockitoBean
    private ViewAssessmentItemResponseUseCase viewAssessmentItemResponseUseCase;
    @MockitoBean
    private ViewAssessmentItemResponseEvaluationUseCase viewAssessmentItemResponseEvaluationUseCase;
    @MockitoBean
    private GetAssessmentRecordsUseCase getAssessmentRecordsUseCase;
    @MockitoBean
    private GetAssessmentRecordingPlaybackUseCase getAssessmentRecordingPlaybackUseCase;
    @MockitoBean
    private ViewAssessmentSessionProctoringAlertsUseCase viewAssessmentSessionProctoringAlertsUseCase;

    private static AssessmentItemResponseTurnResponse turn() {
        return new AssessmentItemResponseTurnResponse(UUID.randomUUID(), ID, 1, TurnType.FOLLOWUP, "Prompt", null, "Text", 12, 3,
            Instant.parse("2026-08-01T01:20:00Z"), null);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void session_should_return_status_enum_and_date_time_values() {
        when(viewAssessmentSessionUseCase.execute(any())).thenReturn(new AssessmentSessionResponse(
            ID, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), STARTED_AT, null, AssessmentSessionStatus.IN_PROGRESS,
            false, null, false));

        graphQlTester.document("{ assessmentSession(id: \"%s\") { status startedAt submittedAt } }".formatted(ID)).execute()
            .errors().verify()
            .path("assessmentSession.status").entity(String.class).isEqualTo("IN_PROGRESS")
            .path("assessmentSession.startedAt").entity(String.class).isEqualTo("2026-08-01T01:00:00Z")
            .path("assessmentSession.submittedAt").valueIsNull();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void answer_should_return_date_time_values_and_turn_type_enum() {
        when(viewAssessmentItemResponseUseCase.execute(any())).thenReturn(new AssessmentItemResponseDetailsResponse(
            ID, UUID.randomUUID(), UUID.randomUUID(), null, null, null, SUBMITTED_AT, List.of(turn())));

        graphQlTester.document("""
            { assessmentItemResponse(answerId: "%s") { submittedAt turns { turnType answeredAt createdAt } } }""".formatted(ID)).execute()
            .errors().verify()
            .path("assessmentItemResponse.submittedAt").entity(String.class).isEqualTo("2026-08-01T02:15:30Z")
            .path("assessmentItemResponse.turns[0].turnType").entity(String.class).isEqualTo("FOLLOWUP")
            .path("assessmentItemResponse.turns[0].answeredAt").entity(String.class).isEqualTo("2026-08-01T01:20:00Z")
            .path("assessmentItemResponse.turns[0].createdAt").valueIsNull();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void evaluation_should_return_engine_and_status_enums_and_date_time_values() {
        AssessmentItemAiEvaluationContextResponse ai = new AssessmentItemAiEvaluationContextResponse(UUID.randomUUID(),
            AssessmentEvaluationEngineType.AI_ENSEMBLE, "model", "v1", BigDecimal.ONE, false, null, false, false,
            SUBMITTED_AT, null, null, null, null);
        AssessmentItemEvaluationTurnResponse evaluationTurn = new AssessmentItemEvaluationTurnResponse(UUID.randomUUID(), 1,
            TurnType.MAIN, "Prompt", null, null, null, null, null, null, null);
        when(viewAssessmentItemResponseEvaluationUseCase.execute(any())).thenReturn(new AssessmentItemEvaluationDetailsResponse(
            ID, UUID.randomUUID(), UUID.randomUUID(), AssessmentEvaluationEngineType.HUMAN, null, null, null, null, null,
            false, null, false, false, AssessmentItemEvaluationStatus.FINALIZED, STARTED_AT, null, null, null, null,
            List.of(), List.of(evaluationTurn), ai));

        graphQlTester.document("""
            { assessmentItemResponseEvaluation(answerId: "%s") {
                engineType status evaluatedAt turns { turnType } ai { engineType evaluatedAt } } }""".formatted(ID)).execute()
            .errors().verify()
            .path("assessmentItemResponseEvaluation.engineType").entity(String.class).isEqualTo("HUMAN")
            .path("assessmentItemResponseEvaluation.status").entity(String.class).isEqualTo("FINALIZED")
            .path("assessmentItemResponseEvaluation.evaluatedAt").entity(String.class).isEqualTo("2026-08-01T01:00:00Z")
            .path("assessmentItemResponseEvaluation.turns[0].turnType").entity(String.class).isEqualTo("MAIN")
            .path("assessmentItemResponseEvaluation.ai.engineType").entity(String.class).isEqualTo("AI_ENSEMBLE")
            .path("assessmentItemResponseEvaluation.ai.evaluatedAt").entity(String.class).isEqualTo("2026-08-01T02:15:30Z");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void my_results_should_return_status_enums_and_date_time_values() {
        when(viewMyAssessmentResultsUseCase.execute(any())).thenReturn(List.of(new StudentAssessmentResultSummaryResponse(
            ID, UUID.randomUUID(), "AS-01", "Midterm", "CENTRALIZED", UUID.randomUUID(), UUID.randomUUID(),
            AssessmentSessionStatus.GRADED, false, STARTED_AT, SUBMITTED_AT, BigDecimal.TEN, null, null,
            AssessmentCandidateResultStatus.PASSED, null, null, null)));

        graphQlTester.document("{ myAssessmentResults { sessionStatus resultStatus startedAt submittedAt } }").execute()
            .errors().verify()
            .path("myAssessmentResults[0].sessionStatus").entity(String.class).isEqualTo("GRADED")
            .path("myAssessmentResults[0].resultStatus").entity(String.class).isEqualTo("PASSED")
            .path("myAssessmentResults[0].startedAt").entity(String.class).isEqualTo("2026-08-01T01:00:00Z")
            .path("myAssessmentResults[0].submittedAt").entity(String.class).isEqualTo("2026-08-01T02:15:30Z");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void recordings_should_return_enums_and_date_time_values_and_bind_the_stream_type_enum() {
        when(getAssessmentRecordsUseCase.execute(any())).thenReturn(List.of(new AssessmentRecordingDto(UUID.randomUUID(), ID, null,
            AssessmentRequiredStreamType.SCREEN, AssessmentRecordingAssemblyStatus.READY, 10L, 60, "WEBRTC", true,
            STARTED_AT, null)));

        graphQlTester.document("""
            query($streamType: AssessmentRecordingStreamType) {
                recordings(assessmentSessionId: "%s", streamType: $streamType) { streamType status createdAt assembledAt }
            }""".formatted(ID)).variable("streamType", "SCREEN").execute()
            .errors().verify()
            .path("recordings[0].streamType").entity(String.class).isEqualTo("SCREEN")
            .path("recordings[0].status").entity(String.class).isEqualTo("READY")
            .path("recordings[0].createdAt").entity(String.class).isEqualTo("2026-08-01T01:00:00Z")
            .path("recordings[0].assembledAt").valueIsNull();
        verify(getAssessmentRecordsUseCase).execute(new GetAssessmentRecordsQuery(ID, "SCREEN"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void recording_playback_should_return_the_stream_type_and_status_enums() {
        when(getAssessmentRecordingPlaybackUseCase.execute(any())).thenReturn(List.of(new AssessmentRecordingPlaybackResponse(
            UUID.randomUUID(), AssessmentRequiredStreamType.CAMERA, AssessmentRecordingAssemblyStatus.PROCESSING, "WEBRTC", null, false, null)));

        graphQlTester.document("{ assessmentRecordingPlayback(assessmentSessionId: \"%s\") { streamType status } }".formatted(ID)).execute()
            .errors().verify()
            .path("assessmentRecordingPlayback[0].streamType").entity(String.class).isEqualTo("CAMERA")
            .path("assessmentRecordingPlayback[0].status").entity(String.class).isEqualTo("PROCESSING");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void recordings_should_reject_an_unknown_stream_type_as_a_validation_error() {
        graphQlTester.document("{ recordings(assessmentSessionId: \"%s\", streamType: NOT_A_STREAM) { id } }".formatted(ID)).execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("NOT_A_STREAM"))
            .verify();

        verifyNoInteractions(getAssessmentRecordsUseCase);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void proctoring_alerts_should_return_captured_and_raised_date_time_values() {
        when(viewAssessmentSessionProctoringAlertsUseCase.execute(any())).thenReturn(List.of(new AssessmentProctoringAlertDto(
            UUID.randomUUID(), "event-1", ID, null, null, "camera", "FACE_MISSING", "WARN", "AI", null, null, STARTED_AT, SUBMITTED_AT)));

        graphQlTester.document("{ assessmentSessionProctoringAlerts(assessmentSessionId: \"%s\") { capturedAt raisedAt } }".formatted(ID))
            .execute()
            .errors().verify()
            .path("assessmentSessionProctoringAlerts[0].capturedAt").entity(String.class).isEqualTo("2026-08-01T01:00:00Z")
            .path("assessmentSessionProctoringAlerts[0].raisedAt").entity(String.class).isEqualTo("2026-08-01T02:15:30Z");
    }
}
