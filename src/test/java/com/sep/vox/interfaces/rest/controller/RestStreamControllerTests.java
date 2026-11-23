package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.time.Instant;
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

import com.sep.vox.application.port.input.usecase.stream.IssueMonitorTokenUseCase;
import com.sep.vox.application.port.input.usecase.stream.IssueStudentStreamTokenUseCase;
import com.sep.vox.application.response.input.stream.IssueStudentStreamTokenResponse;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.support.RestControllerTest;

@RestControllerTest(RestStreamController.class)
class RestStreamControllerTests {

    private static final String STUDENT_TOKEN_URI = "/api/v1/streams/student/token";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private IssueStudentStreamTokenUseCase issueStudentStreamTokenUseCase;
    @MockitoBean
    private IssueMonitorTokenUseCase issueMonitorTokenUseCase;

    @Test
    @WithAnonymousUser
    void studentToken_should_answer_401_for_an_anonymous_caller() {
        assertThat(postToken(streamBody("\"CAMERA\""))).hasStatus(401);
        verifyNoInteractions(issueStudentStreamTokenUseCase);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void studentToken_should_answer_403_for_a_teacher() {
        assertThat(postToken(streamBody("\"CAMERA\""))).hasStatus(403);
        verifyNoInteractions(issueStudentStreamTokenUseCase);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentToken_should_accept_a_stream_type_in_any_letter_case() {
        when(issueStudentStreamTokenUseCase.execute(any())).thenReturn(new IssueStudentStreamTokenResponse(
            "token", UUID.randomUUID(), UUID.randomUUID(), List.of("camera"), Instant.parse("2026-08-01T02:00:00Z")));

        MvcTestResult result = postToken(streamBody("\"camera_and_screen\""));

        assertThat(result).hasStatusOk();
        verify(issueStudentStreamTokenUseCase).execute(argThat(command ->
            command.streamType() == AssessmentRequiredStreamType.CAMERA_AND_SCREEN));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentToken_should_pass_a_missing_stream_type_as_null() {
        when(issueStudentStreamTokenUseCase.execute(any())).thenReturn(new IssueStudentStreamTokenResponse(
            "token", UUID.randomUUID(), UUID.randomUUID(), List.of("camera"), Instant.parse("2026-08-01T02:00:00Z")));

        MvcTestResult result = postToken(streamBody("null"));

        assertThat(result).hasStatusOk();
        verify(issueStudentStreamTokenUseCase).execute(argThat(command -> command.streamType() == null));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentToken_should_answer_400_for_an_unknown_stream_type() {
        MvcTestResult result = postToken(streamBody("\"HOLOGRAM\""));

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.streamType").asString()
            .contains("CAMERA", "SCREEN", "CAMERA_AND_SCREEN");
        verifyNoInteractions(issueStudentStreamTokenUseCase);
    }

    private static String streamBody(String streamTypeJson) {
        return "{\"assessmentSessionId\": \"" + UUID.randomUUID() + "\", \"streamType\": " + streamTypeJson + "}";
    }

    private MvcTestResult postToken(String body) {
        return mvc.post().uri(STUDENT_TOKEN_URI).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }
}
