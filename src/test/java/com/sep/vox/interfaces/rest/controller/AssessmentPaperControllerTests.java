package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sep.vox.application.port.input.usecase.assessmentpaper.CreateAssessmentPaperUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpaper.DeleteAssessmentPaperUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpaper.UpdateAssessmentPaperItemUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpaper.UpdateAssessmentPaperSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpaper.UpdateAssessmentPaperStatusUseCase;
import com.sep.vox.domain.dto.AssessmentPaperDto;
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
 * {@code domain/dto} records returned by {@link AssessmentPaperController}. The GraphQL typed-scalars plan changes those
 * field types; the JSON written here must stay the same.
 */
@RestControllerTest(AssessmentPaperController.class)
class AssessmentPaperControllerTests {

    private static final String CREATED_AT = "2026-07-28T02:30:00Z";
    private static final String UPDATED_AT = "2026-07-29T03:45:10Z";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CreateAssessmentPaperUseCase createAssessmentPaperUseCase;
    @MockitoBean
    private UpdateAssessmentPaperItemUseCase updateAssessmentPaperItemUseCase;
    @MockitoBean
    private UpdateAssessmentPaperSectionUseCase updateAssessmentPaperSectionUseCase;
    @MockitoBean
    private UpdateAssessmentPaperStatusUseCase updateAssessmentPaperStatusUseCase;
    @MockitoBean
    private DeleteAssessmentPaperUseCase deleteAssessmentPaperUseCase;

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateStatus_should_keep_paper_json_text() {
        when(updateAssessmentPaperStatusUseCase.execute(any())).thenReturn(
            new AssessmentPaperDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "P-01", 1, AssessmentPaperStatus.IN_REVIEW, 600, Instant.parse(CREATED_AT), Instant.parse(UPDATED_AT), UUID.randomUUID(), UUID.randomUUID()));

        MvcTestResult result = mvc.patch().uri("/api/v1/assessment-papers/{id}/status", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"action\": \"SUBMIT\"}")
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "status", "IN_REVIEW",
                "createdAt", CREATED_AT,
                "updatedAt", UPDATED_AT
            ));
    }
}
