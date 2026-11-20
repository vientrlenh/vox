package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.sep.vox.application.port.input.usecase.assessment.GetAssessmentSessionPaperUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.CompleteAssessmentSessionGradingUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.CreateAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.DeleteAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.FlagAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.ForceEndAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.HandOffGradingToHumanUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.ReportAiUsageUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.RetryGradingAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateAssessmentSessionRemainingTimeUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateAssessmentSessionStatusUseCase;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import java.time.Instant;
import com.sep.vox.application.response.input.assessmentsession.AssessmentSessionResponse;
import com.sep.vox.support.RestControllerTest;

/**
 * Pins the JSON text that REST clients receive for the temporal and status fields of the
 * {@link AssessmentSessionResponse} returned by {@link AssessmentSessionController}. The GraphQL typed-scalars plan changes
 * those field types; the JSON written here must stay the same.
 */
@RestControllerTest(AssessmentSessionController.class)
class AssessmentSessionControllerTests {

    private static final String STARTED_AT = "2026-08-01T01:00:00Z";
    private static final String SUBMITTED_AT = "2026-08-01T02:15:30Z";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CreateAssessmentSessionUseCase createAssessmentSessionUseCase;
    @MockitoBean
    private CompleteAssessmentSessionGradingUseCase completeAssessmentSessionGradingUseCase;
    @MockitoBean
    private UpdateAssessmentSessionStatusUseCase updateAssessmentSessionStatusUseCase;
    @MockitoBean
    private GetAssessmentSessionPaperUseCase getAssessmentSessionPaperUseCase;
    @MockitoBean
    private DeleteAssessmentSessionUseCase deleteAssessmentSessionUseCase;
    @MockitoBean
    private FlagAssessmentSessionUseCase flagAssessmentSessionUseCase;
    @MockitoBean
    private ForceEndAssessmentSessionUseCase forceEndAssessmentSessionUseCase;
    @MockitoBean
    private RetryGradingAssessmentSessionUseCase retryGradingAssessmentSessionUseCase;
    @MockitoBean
    private HandOffGradingToHumanUseCase handOffGradingToHumanUseCase;
    @MockitoBean
    private UpdateAssessmentSessionRemainingTimeUseCase updateAssessmentSessionRemainingTimeUseCase;
    @MockitoBean
    private ReportAiUsageUseCase reportAiUsageUseCase;

    @Test
    @WithMockUser(roles = "STUDENT")
    void updateStatus_should_keep_session_json_text() {
        when(updateAssessmentSessionStatusUseCase.execute(any())).thenReturn(new AssessmentSessionResponse(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.parse(STARTED_AT), Instant.parse(SUBMITTED_AT), AssessmentSessionStatus.SUBMITTED, false, null, false));

        MvcTestResult result = mvc.patch().uri("/api/v1/assessment-sessions/{id}", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\": \"SUBMITTED\"}")
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "status", "SUBMITTED",
                "startedAt", STARTED_AT,
                "submittedAt", SUBMITTED_AT
            ));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void updateStatus_should_answer_400_listing_statuses_for_unknown_value() {
        MvcTestResult result = mvc.patch().uri("/api/v1/assessment-sessions/{id}", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\": \"NOPE\"}")
            .exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.status").asString()
            .contains("IN_PROGRESS", "SUBMITTED");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void updateStatus_should_answer_400_when_status_is_missing() {
        MvcTestResult result = mvc.patch().uri("/api/v1/assessment-sessions/{id}", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}")
            .exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.status").isNotNull();
    }
}
