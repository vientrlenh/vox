package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sep.vox.application.port.input.usecase.assessmentschedule.AddAssessmentScheduleProctorUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.CreateAssessmentScheduleUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.DeleteAssessmentScheduleUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.RemoveAssessmentScheduleProctorUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.UpdateAssessmentScheduleStatusUseCase;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
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
 * {@code domain/dto} records returned by {@link AssessmentScheduleController}. The GraphQL typed-scalars plan changes those
 * field types; the JSON written here must stay the same.
 */
@RestControllerTest(AssessmentScheduleController.class)
class AssessmentScheduleControllerTests {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CreateAssessmentScheduleUseCase createAssessmentScheduleUseCase;
    @MockitoBean
    private UpdateAssessmentScheduleStatusUseCase updateAssessmentScheduleStatusUseCase;
    @MockitoBean
    private DeleteAssessmentScheduleUseCase deleteAssessmentScheduleUseCase;
    @MockitoBean
    private AddAssessmentScheduleProctorUseCase addAssessmentScheduleProctorUseCase;
    @MockitoBean
    private RemoveAssessmentScheduleProctorUseCase removeAssessmentScheduleProctorUseCase;

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateStatus_should_keep_schedule_json_text() {
        when(updateAssessmentScheduleStatusUseCase.execute(any())).thenReturn(
            new AssessmentScheduleDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-08-01T01:00:00Z"), Instant.parse("2026-08-01T03:00:00Z"), AssessmentScheduleStatus.PUBLISHED, null));

        MvcTestResult result = mvc.patch().uri("/api/v1/assessments/{assessmentId}/schedules/{scheduleId}/status", UUID.randomUUID(), UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"action\": \"PUBLISH\"}")
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "startDate", "2026-08-01T01:00:00Z",
                "endDate", "2026-08-01T03:00:00Z",
                "status", "PUBLISHED"
            ));
    }
}
