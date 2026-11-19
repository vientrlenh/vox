package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.sep.vox.application.port.input.command.BulkFinalizeExamResultsCommand;
import com.sep.vox.application.port.input.query.ExportExamScoresQuery;
import com.sep.vox.application.port.input.usecase.examgrading.BulkFinalizeExamResultsUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ExportExamScoresExcelUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ExportExamScoresUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.PreviewBulkFinalizeUseCase;
import com.sep.vox.support.RestControllerTest;

/**
 * HTTP layer of the two score export routes.
 *
 * <p>Two things break only here and no other test reaches them: (1) whether the query-string
 * filters arrive at the use case - a missing field makes the exported file differ from the table the
 * user is looking at; (2) the {@code Content-Disposition} header and content type - a wrong one makes
 * the browser open the file as text instead of downloading it.
 */
@RestControllerTest(ExamResultController.class)
@WithMockUser(roles = "TEACHER")
class ExamResultControllerTests {

    private static final String EXCEL_URI = "/api/v1/exam-results/export/excel";
    private static final String CSV_URI = "/api/v1/exam-results/export";
    private static final String XLSX_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final UUID examId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ExportExamScoresUseCase exportExamScoresUseCase;
    @MockitoBean
    private ExportExamScoresExcelUseCase exportExamScoresExcelUseCase;
    @MockitoBean
    private PreviewBulkFinalizeUseCase previewBulkFinalizeUseCase;
    @MockitoBean
    private BulkFinalizeExamResultsUseCase bulkFinalizeExamResultsUseCase;

    @BeforeEach
    void stubExports() {
        when(exportExamScoresUseCase.execute(any())).thenReturn("Mã bài\r\n");
        when(exportExamScoresExcelUseCase.execute(any())).thenReturn(new byte[] {80, 75, 3, 4});
        when(exportExamScoresExcelUseCase.exportFileName(any()))
            .thenReturn("bang-diem-lop-20260806120000.xlsx");
    }

    @Test
    void should_forward_every_screen_filter_to_the_excel_use_case() {
        var result = mvc.get().uri(EXCEL_URI)
            .param("examId", examId.toString())
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
        verify(exportExamScoresExcelUseCase).execute(new ExportExamScoresQuery(examId, scheduleId, teacherId,
            "RELEASED", "APPEAL", "COMPLETED", true, true, Boolean.TRUE, "nguyen", "CLASS_TEST"));
    }

    /** {@code null} means no filter, which is different from {@code false}: only exams WITHOUT an open appeal. */
    @Test
    void should_keep_an_absent_open_appeal_filter_null() {
        mvc.get().uri(EXCEL_URI).param("examId", examId.toString()).exchange();

        verify(exportExamScoresExcelUseCase).execute(new ExportExamScoresQuery(examId, null, null, null, null,
            null, false, false, null, null, null));
    }

    @Test
    void should_send_the_workbook_as_a_download_with_the_xlsx_content_type() {
        var result = mvc.get().uri(EXCEL_URI).param("examId", examId.toString()).param("kind", "CLASS_TEST")
            .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).headers().hasValue(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"bang-diem-lop-20260806120000.xlsx\"");
        assertThat(result).contentType().isCompatibleWith(MediaType.parseMediaType(XLSX_TYPE));
        // The first four bytes of every .xlsx are the ZIP magic number.
        assertThat(result).body().startsWith((byte) 80, (byte) 75, (byte) 3, (byte) 4);
    }

    /** The file name depends on the exam kind, so {@code kind} must reach the place that names the file. */
    @Test
    void should_name_the_file_after_the_requested_kind() {
        mvc.get().uri(EXCEL_URI).param("examId", examId.toString()).param("kind", "CLASS_TEST").exchange();

        verify(exportExamScoresExcelUseCase).exportFileName("CLASS_TEST");
    }

    @Test
    void should_forward_the_same_filters_on_the_csv_route() {
        mvc.get().uri(CSV_URI)
            .param("examId", examId.toString())
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

        verify(exportExamScoresUseCase).execute(new ExportExamScoresQuery(examId, null, teacherId, "RELEASED",
            "APPEAL", "COMPLETED", true, true, Boolean.FALSE, "nguyen", "CENTRALIZED"));
    }

    @Test
    void should_send_the_csv_as_utf8_so_vietnamese_survives() {
        var result = mvc.get().uri(CSV_URI).param("examId", examId.toString()).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).headers().hasValue(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8");
        assertThat(result).headers().hasValue(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"bang-diem.csv\"");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void finalize_should_pass_the_confirmation_flag_to_the_use_case() {
        when(bulkFinalizeExamResultsUseCase.execute(new BulkFinalizeExamResultsCommand(examId, true)))
            .thenReturn(7);

        var result = mvc.post().uri("/api/v1/exam-results/finalize")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"examId\": \"" + examId + "\", \"releasePendingWithAiScores\": true}")
            .exchange();

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.data").isEqualTo(7);
    }

    @Test
    void finalize_should_return_bad_request_when_exam_id_is_missing() {
        var result = mvc.post().uri("/api/v1/exam-results/finalize")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}")
            .exchange();

        assertThat(result).hasStatus(400);
        verifyNoInteractions(bulkFinalizeExamResultsUseCase);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void export_should_return_403_for_a_student() {
        var result = mvc.get().uri(EXCEL_URI).param("examId", examId.toString()).exchange();

        assertThat(result).hasStatus(403);
        verifyNoInteractions(exportExamScoresExcelUseCase);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void preview_should_return_403_for_a_student() {
        var result = mvc.get().uri("/api/v1/exam-results/finalize/preview")
            .param("examId", examId.toString()).exchange();

        assertThat(result).hasStatus(403);
        verifyNoInteractions(previewBulkFinalizeUseCase);
    }
}
