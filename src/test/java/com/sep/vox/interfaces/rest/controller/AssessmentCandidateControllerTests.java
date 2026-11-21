package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sep.vox.application.port.input.usecase.assessmentcandidate.AddAssessmentCandidateUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.AssignAssessmentCandidateScheduleUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.AssignAssessmentPapersUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.AutoFillAssessmentCandidatesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.BulkAssignAssessmentCandidateScheduleUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.BulkDeleteAssessmentCandidatesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.DeleteAssessmentCandidateUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.ImportAssessmentCandidatesFromClassUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.ImportAssessmentCandidatesFromGradeUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.UnblockAssessmentCandidateUseCase;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.UpdateAssessmentCandidateStatusUseCase;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.support.RestControllerTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Pins the JSON text that REST clients receive for the temporal and status fields of the shared
 * {@code domain/dto} records returned by {@link AssessmentCandidateController}. The GraphQL typed-scalars plan changes those
 * field types; the JSON written here must stay the same.
 */
@RestControllerTest(AssessmentCandidateController.class)
class AssessmentCandidateControllerTests {

    private static final String UPDATED_AT = "2026-07-29T03:45:10Z";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AddAssessmentCandidateUseCase addAssessmentCandidateUseCase;
    @MockitoBean
    private ImportAssessmentCandidatesFromClassUseCase importAssessmentCandidatesFromClassUseCase;
    @MockitoBean
    private ImportAssessmentCandidatesFromGradeUseCase importAssessmentCandidatesFromGradeUseCase;
    @MockitoBean
    private AssignAssessmentCandidateScheduleUseCase assignAssessmentCandidateScheduleUseCase;
    @MockitoBean
    private BulkAssignAssessmentCandidateScheduleUseCase bulkAssignAssessmentCandidateScheduleUseCase;
    @MockitoBean
    private AutoFillAssessmentCandidatesUseCase autoFillAssessmentCandidatesUseCase;
    @MockitoBean
    private AssignAssessmentPapersUseCase assignAssessmentPapersUseCase;
    @MockitoBean
    private UpdateAssessmentCandidateStatusUseCase updateAssessmentCandidateStatusUseCase;
    @MockitoBean
    private UnblockAssessmentCandidateUseCase unblockAssessmentCandidateUseCase;
    @MockitoBean
    private DeleteAssessmentCandidateUseCase deleteAssessmentCandidateUseCase;
    @MockitoBean
    private BulkDeleteAssessmentCandidatesUseCase bulkDeleteAssessmentCandidatesUseCase;

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateStatus_should_keep_candidate_json_text() {
        when(updateAssessmentCandidateStatusUseCase.execute(any())).thenReturn(
            new AssessmentCandidateDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AssessmentCandidateStatus.ATTENDED, Instant.parse("2026-08-01T01:00:00Z"), Instant.parse(UPDATED_AT), Instant.parse("2026-08-01T03:00:00Z")));

        MvcTestResult result = mvc.put().uri("/api/v1/assessments/{assessmentId}/candidates/{candidateId}/status", UUID.randomUUID(), UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\": \"ATTENDED\"}")
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "status", "ATTENDED",
                "assignedAt", "2026-08-01T01:00:00Z",
                "updatedAt", UPDATED_AT,
                "blockedAt", "2026-08-01T03:00:00Z"
            ));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateStatus_should_answer_400_listing_statuses_for_unknown_value() {
        MvcTestResult result = mvc.put().uri("/api/v1/assessments/{assessmentId}/candidates/{candidateId}/status", UUID.randomUUID(), UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\": \"NOPE\"}")
            .exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.status").asString()
            .contains("ASSIGNED", "ATTENDED");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateStatus_should_answer_400_when_status_is_missing() {
        MvcTestResult result = mvc.put().uri("/api/v1/assessments/{assessmentId}/candidates/{candidateId}/status", UUID.randomUUID(), UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}")
            .exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.status").isNotNull();
    }
}
