package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.sep.vox.application.port.input.usecase.assessmentappeal.ApproveAndClaimAssessmentAppealUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.ApproveAssessmentAppealUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.AssignAssessmentAppealReviewerUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.CreateAssessmentAppealUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.RejectAssessmentAppealUseCase;
import com.sep.vox.application.port.input.usecase.assessmentappeal.WithdrawAssessmentAppealUseCase;
import com.sep.vox.support.RestControllerTest;

@RestControllerTest(AssessmentAppealController.class)
class AssessmentAppealControllerTests {

    private static final String APPROVE_URI = "/api/v1/assessment-appeals/{appealId}/approve";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CreateAssessmentAppealUseCase createAssessmentAppealUseCase;
    @MockitoBean
    private ApproveAssessmentAppealUseCase approveAssessmentAppealUseCase;
    @MockitoBean
    private ApproveAndClaimAssessmentAppealUseCase approveAndClaimAssessmentAppealUseCase;
    @MockitoBean
    private RejectAssessmentAppealUseCase rejectAssessmentAppealUseCase;
    @MockitoBean
    private AssignAssessmentAppealReviewerUseCase assignAssessmentAppealReviewerUseCase;
    @MockitoBean
    private WithdrawAssessmentAppealUseCase withdrawAssessmentAppealUseCase;

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void approve_should_bind_deadline_as_iso_instant() {
        when(approveAssessmentAppealUseCase.execute(any())).thenReturn(UUID.randomUUID());

        MvcTestResult result = approveWith("{\"deadline\": \"2030-08-01T10:00:00+07:00\"}");

        assertThat(result).hasStatusOk();
        verify(approveAssessmentAppealUseCase).execute(
            argThat(command -> command.deadline().equals(Instant.parse("2030-08-01T03:00:00Z"))));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void approve_should_answer_400_naming_deadline_for_non_iso_text() {
        assertThat(approveWith("{\"deadline\": \"01/08/2030\"}"))
            .hasStatus(400).bodyJson().extractingPath("$.errors.deadline").isNotNull();
        assertThat(approveWith("{\"deadline\": \"2030-08-01\"}"))
            .hasStatus(400).bodyJson().extractingPath("$.errors.deadline").isNotNull();
        verify(approveAssessmentAppealUseCase, never()).execute(any());
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void approve_should_answer_400_naming_deadline_when_missing() {
        assertThat(approveWith("{}"))
            .hasStatus(400).bodyJson().extractingPath("$.errors.deadline").isNotNull();
    }

    @Test
    @WithAnonymousUser
    void approve_should_answer_401_for_anonymous_caller() {
        assertThat(approveWith("{\"deadline\": \"2030-08-01T03:00:00Z\"}")).hasStatus(401);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void approve_should_answer_403_for_student() {
        assertThat(approveWith("{\"deadline\": \"2030-08-01T03:00:00Z\"}")).hasStatus(403);
        verify(approveAssessmentAppealUseCase, never()).execute(any());
    }

    private MvcTestResult approveWith(String body) {
        return mvc.post().uri(APPROVE_URI, UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .exchange();
    }
}
