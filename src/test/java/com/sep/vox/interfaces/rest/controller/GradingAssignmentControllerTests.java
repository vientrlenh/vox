package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.sep.vox.application.port.input.usecase.assessmentgrading.AssignGradingUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.AutoAssignGradingUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ClearInvalidResultUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.DeclineGradingAssignmentUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.InvalidateResultUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.PreviewGradingUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ReassignGradingUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ReclaimOverdueAssignmentsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.RegradeResultUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.RemoveGradingAssignmentUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.SetGradingDeadlineUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.UpholdResultUseCase;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.GradingSampleSelectionMode;
import com.sep.vox.support.RestControllerTest;

@RestControllerTest(GradingAssignmentController.class)
class GradingAssignmentControllerTests {

    private static final String ASSIGN_URI = "/api/v1/grading-assignments";
    private static final String AUTO_ASSIGN_URI = "/api/v1/grading-assignments/auto";
    private static final String TEACHER_ID = "00000000-0000-0000-0000-000000000001";
    private static final String RESULT_ID = "00000000-0000-0000-0000-000000000002";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AssignGradingUseCase assignGradingUseCase;
    @MockitoBean
    private AutoAssignGradingUseCase autoAssignGradingUseCase;
    @MockitoBean
    private ReassignGradingUseCase reassignGradingUseCase;
    @MockitoBean
    private RemoveGradingAssignmentUseCase removeGradingAssignmentUseCase;
    @MockitoBean
    private PreviewGradingUseCase previewGradingUseCase;
    @MockitoBean
    private UpholdResultUseCase upholdResultUseCase;
    @MockitoBean
    private RegradeResultUseCase regradeResultUseCase;
    @MockitoBean
    private InvalidateResultUseCase invalidateResultUseCase;
    @MockitoBean
    private ClearInvalidResultUseCase clearInvalidResultUseCase;
    @MockitoBean
    private DeclineGradingAssignmentUseCase declineGradingAssignmentUseCase;
    @MockitoBean
    private SetGradingDeadlineUseCase setGradingDeadlineUseCase;
    @MockitoBean
    private ReclaimOverdueAssignmentsUseCase reclaimOverdueAssignmentsUseCase;

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void assign_should_bind_round_type_in_any_letter_case() {
        when(assignGradingUseCase.execute(any())).thenReturn(List.of(UUID.randomUUID()));

        MvcTestResult result = postAssign("\"spot_check\"");

        assertThat(result).hasStatus(201);
        verify(assignGradingUseCase).execute(argThat(command -> command.roundType() == GradingRoundType.SPOT_CHECK));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void assign_should_answer_400_listing_rounds_for_unknown_round_type() {
        MvcTestResult result = postAssign("\"NOPE\"");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.roundType").asString()
            .contains("INITIAL", "SPOT_CHECK", "REMEDIATION", "APPEAL")
            .doesNotContain("GradingRoundType");
        verify(assignGradingUseCase, never()).execute(any());
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void assign_should_answer_400_when_round_type_is_missing() {
        MvcTestResult result = postAssign("null");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.roundType").isNotNull();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void autoAssign_should_bind_round_type_and_selection_mode() {
        when(autoAssignGradingUseCase.execute(any())).thenReturn(List.of(UUID.randomUUID()));

        MvcTestResult result = postAutoAssign("\"INITIAL\"", "\"risk_based\"");

        assertThat(result).hasStatus(201);
        verify(autoAssignGradingUseCase).execute(argThat(command ->
            command.roundType() == GradingRoundType.INITIAL
                && command.selectionMode() == GradingSampleSelectionMode.RISK_BASED));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void autoAssign_should_pass_null_selection_mode_when_omitted() {
        when(autoAssignGradingUseCase.execute(any())).thenReturn(List.of(UUID.randomUUID()));

        MvcTestResult result = postAutoAssign("\"INITIAL\"", "null");

        assertThat(result).hasStatus(201);
        verify(autoAssignGradingUseCase).execute(argThat(command -> command.selectionMode() == null));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void autoAssign_should_answer_400_listing_modes_for_unknown_selection_mode() {
        MvcTestResult result = postAutoAssign("\"INITIAL\"", "\"NOPE\"");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.selectionMode").asString()
            .contains("ALL", "RANDOM_PERCENT", "RISK_BASED", "MANUAL_LIST");
        verify(autoAssignGradingUseCase, never()).execute(any());
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void autoAssign_should_answer_400_for_unknown_round_type() {
        MvcTestResult result = postAutoAssign("\"NOPE\"", "\"ALL\"");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.roundType").isNotNull();
    }

    @Test
    @WithAnonymousUser
    void assign_should_answer_401_for_anonymous_caller() {
        assertThat(postAssign("\"INITIAL\"")).hasStatus(401);
        assertThat(postAutoAssign("\"INITIAL\"", "\"ALL\"")).hasStatus(401);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void assign_should_answer_403_for_teacher() {
        assertThat(postAssign("\"INITIAL\"")).hasStatus(403);
        assertThat(postAutoAssign("\"INITIAL\"", "\"ALL\"")).hasStatus(403);
        verify(assignGradingUseCase, never()).execute(any());
    }

    private MvcTestResult postAssign(String roundTypeJson) {
        return mvc.post().uri(ASSIGN_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"roundType\": " + roundTypeJson + ", \"assignments\": [{\"candidateResultId\": \""
                + RESULT_ID + "\", \"teacherId\": \"" + TEACHER_ID + "\"}]}")
            .exchange();
    }

    private MvcTestResult postAutoAssign(String roundTypeJson, String selectionModeJson) {
        return mvc.post().uri(AUTO_ASSIGN_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"roundType\": " + roundTypeJson + ", \"selectionMode\": " + selectionModeJson
                + ", \"teacherIds\": [\"" + TEACHER_ID + "\"]}")
            .exchange();
    }
}
