package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.sep.vox.application.port.input.command.BulkFinalizeAssessmentResultsCommand;
import com.sep.vox.application.port.input.query.ExportAssessmentScoresQuery;
import com.sep.vox.application.port.input.usecase.assessmentgrading.BulkFinalizeAssessmentResultsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ExportAssessmentScoresExcelUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ExportAssessmentScoresUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.PreviewBulkFinalizeUseCase;
import com.sep.vox.support.RestControllerTest;

/**
 * HTTP layer of the two score export routes.
 *
 * <p>Two things break only here and no other test reaches them: (1) whether the query-string
 * filters arrive at the use case - a missing field makes the exported file differ from the table the
 * user is looking at; (2) the {@code Content-Disposition} header and content type - a wrong one makes
 * the browser open the file as text instead of downloading it.
 */
@RestControllerTest(AssessmentResultController.class)
@WithMockUser(roles = "TEACHER")
class AssessmentResultControllerTests {

    private static final String EXCEL_URI = "/api/v1/assessment-results/export/excel";
    private static final String CSV_URI = "/api/v1/assessment-results/export";
    private static final String XLSX_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final UUID assessmentId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ExportAssessmentScoresUseCase exportAssessmentScoresUseCase;
    @MockitoBean
    private ExportAssessmentScoresExcelUseCase exportAssessmentScoresExcelUseCase;
    @MockitoBean
    private PreviewBulkFinalizeUseCase previewBulkFinalizeUseCase;
    @MockitoBean
    private BulkFinalizeAssessmentResultsUseCase bulkFinalizeAssessmentResultsUseCase;

    @BeforeEach
    void stubExports() {
        when(exportAssessmentScoresUseCase.execute(any())).thenReturn("Mã bài\r\n");
        when(exportAssessmentScoresExcelUseCase.execute(any())).thenReturn(new byte[] {80, 75, 3, 4});
        when(exportAssessmentScoresExcelUseCase.exportFileName(any()))
            .thenReturn("bang-diem-lop-20260806120000.xlsx");
    }

    @Test
    void should_forward_every_screen_filter_to_the_excel_use_case() {
        MvcTestResult result = mvc.get().uri(EXCEL_URI)
            .param("assessmentId", assessmentId.toString())
            .param("scheduleId", scheduleId.toString())
            .param("teacherId", teacherId.toString())
            .param("resultStatus", "RELEASED")
            .param("roundType", "APPEAL")
            .param("assignmentStatus", "COMPLETED")
            .param("unassignedOnly", "true")
            .param("overdueOnly", "true")
            .param("hasOpenAppeal", "true")
            .param("keyword", "nguyen")
            .param("kind", "CLASS_TEST")
            .exchange();

        assertThat(result).hasStatusOk();
        verify(exportAssessmentScoresExcelUseCase).execute(new ExportAssessmentScoresQuery(assessmentId, scheduleId, teacherId,
            AssessmentCandidateResultStatus.RELEASED, GradingRoundType.APPEAL, GradingAssignmentStatus.COMPLETED,
            true, true, Boolean.TRUE, "nguyen", AssessmentKind.CLASS_TEST));
    }

    /** {@code null} means no filter, which is different from {@code false}: only assessments WITHOUT an open appeal. */
    @Test
    void should_keep_an_absent_open_appeal_filter_null() {
        mvc.get().uri(EXCEL_URI).param("assessmentId", assessmentId.toString()).exchange();

        verify(exportAssessmentScoresExcelUseCase).execute(new ExportAssessmentScoresQuery(assessmentId, null, null, null, null,
            null, false, false, null, null, null));
    }

    @Test
    void should_send_the_workbook_as_a_download_with_the_xlsx_content_type() {
        MvcTestResult result = mvc.get().uri(EXCEL_URI).param("assessmentId", assessmentId.toString()).param("kind", "CLASS_TEST")
            .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).headers().hasValue(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"bang-diem-lop-20260806120000.xlsx\"");
        assertThat(result).contentType().isCompatibleWith(MediaType.parseMediaType(XLSX_TYPE));
        // The first four bytes of every .xlsx are the ZIP magic number.
        assertThat(result).body().startsWith((byte) 80, (byte) 75, (byte) 3, (byte) 4);
    }

    /** The file name depends on the assessment kind, so {@code kind} must reach the place that names the file. */
    @Test
    void should_name_the_file_after_the_requested_kind() {
        mvc.get().uri(EXCEL_URI).param("assessmentId", assessmentId.toString()).param("kind", "CLASS_TEST").exchange();

        verify(exportAssessmentScoresExcelUseCase).exportFileName(AssessmentKind.CLASS_TEST);
    }

    @Test
    void should_forward_the_same_filters_on_the_csv_route() {
        mvc.get().uri(CSV_URI)
            .param("assessmentId", assessmentId.toString())
            .param("teacherId", teacherId.toString())
            .param("resultStatus", "RELEASED")
            .param("roundType", "APPEAL")
            .param("assignmentStatus", "COMPLETED")
            .param("unassignedOnly", "true")
            .param("overdueOnly", "true")
            .param("hasOpenAppeal", "false")
            .param("keyword", "nguyen")
            .param("kind", "CENTRALIZED")
            .exchange();

        verify(exportAssessmentScoresUseCase).execute(new ExportAssessmentScoresQuery(assessmentId, null, teacherId,
            AssessmentCandidateResultStatus.RELEASED, GradingRoundType.APPEAL, GradingAssignmentStatus.COMPLETED,
            true, true, Boolean.FALSE, "nguyen", AssessmentKind.CENTRALIZED));
    }

    @Test
    void should_send_the_csv_as_utf8_so_vietnamese_survives() {
        MvcTestResult result = mvc.get().uri(CSV_URI).param("assessmentId", assessmentId.toString()).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).headers().hasValue(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8");
        assertThat(result).headers().hasValue(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"bang-diem.csv\"");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void finalize_should_pass_the_confirmation_flag_to_the_use_case() {
        when(bulkFinalizeAssessmentResultsUseCase.execute(new BulkFinalizeAssessmentResultsCommand(assessmentId, true)))
            .thenReturn(7);

        MvcTestResult result = mvc.post().uri("/api/v1/assessment-results/finalize")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"assessmentId\": \"" + assessmentId + "\", \"releasePendingWithAiScores\": true}")
            .exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.data").isEqualTo(7);
    }

    @Test
    void finalize_should_return_bad_request_when_assessment_id_is_missing() {
        MvcTestResult result = mvc.post().uri("/api/v1/assessment-results/finalize")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}")
            .exchange();

        assertThat(result).hasStatus(400);
        verifyNoInteractions(bulkFinalizeAssessmentResultsUseCase);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void export_should_return_403_for_a_student() {
        MvcTestResult result = mvc.get().uri(EXCEL_URI).param("assessmentId", assessmentId.toString()).exchange();

        assertThat(result).hasStatus(403);
        verifyNoInteractions(exportAssessmentScoresExcelUseCase);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void preview_should_return_403_for_a_student() {
        MvcTestResult result = mvc.get().uri("/api/v1/assessment-results/finalize/preview")
            .param("assessmentId", assessmentId.toString()).exchange();

        assertThat(result).hasStatus(403);
        verifyNoInteractions(previewBulkFinalizeUseCase);
    }

    @Test
    void export_should_bind_filter_values_in_any_letter_case() {
        mvc.get().uri(CSV_URI)
            .param("assessmentId", assessmentId.toString())
            .param("resultStatus", "released")
            .param("roundType", "spot_check")
            .param("assignmentStatus", "completed")
            .param("kind", "class_test")
            .exchange();

        verify(exportAssessmentScoresUseCase).execute(new ExportAssessmentScoresQuery(assessmentId, null, null,
            AssessmentCandidateResultStatus.RELEASED, GradingRoundType.SPOT_CHECK, GradingAssignmentStatus.COMPLETED,
            false, false, null, null, AssessmentKind.CLASS_TEST));
    }

    @Test
    void export_should_treat_blank_enum_filters_as_no_filter() {
        mvc.get().uri(CSV_URI)
            .param("assessmentId", assessmentId.toString())
            .param("resultStatus", "")
            .param("roundType", "")
            .param("assignmentStatus", "")
            .param("kind", "")
            .exchange();

        verify(exportAssessmentScoresUseCase).execute(ExportAssessmentScoresQuery.scopedTo(assessmentId, null, null));
    }

    @Test
    void export_should_answer_400_naming_each_unknown_filter_on_both_routes() {
        for (String uri : new String[] {CSV_URI, EXCEL_URI}) {
            assertUnknownFilterAnswers400(uri, "resultStatus", "RELEASED");
            assertUnknownFilterAnswers400(uri, "roundType", "SPOT_CHECK");
            assertUnknownFilterAnswers400(uri, "assignmentStatus", "COMPLETED");
            assertUnknownFilterAnswers400(uri, "kind", "CLASS_TEST");
        }
        verifyNoInteractions(exportAssessmentScoresUseCase, exportAssessmentScoresExcelUseCase);
    }

    private void assertUnknownFilterAnswers400(String uri, String parameter, String acceptedConstant) {
        MvcTestResult result = mvc.get().uri(uri)
            .param("assessmentId", assessmentId.toString())
            .param(parameter, "NOPE")
            .exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors." + parameter).asString()
            .contains(acceptedConstant);
    }
}
