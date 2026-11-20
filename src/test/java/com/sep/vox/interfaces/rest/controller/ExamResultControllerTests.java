package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;

import com.sep.vox.application.port.input.query.ExportExamScoresQuery;
import com.sep.vox.application.port.input.usecase.examgrading.BulkFinalizeExamResultsUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ExportExamScoresExcelUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ExportExamScoresUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.PreviewBulkFinalizeUseCase;

/**
 * Tầng HTTP của hai đường xuất bảng điểm.
 *
 * <p>Hai thứ chỉ hỏng ở đúng đây và không test nào khác chạm tới: (1) bộ lọc trên query
 * string có tới được use case không — thiếu một trường là file xuất ra khác hẳn bảng người
 * dùng đang nhìn; (2) header {@code Content-Disposition} và content type — sai là trình
 * duyệt mở file như text thay vì tải về.
 */
class ExamResultControllerTests {

    private ExportExamScoresUseCase exportExamScoresUseCase;
    private ExportExamScoresExcelUseCase exportExamScoresExcelUseCase;
    private ExamResultController controller;

    private final UUID examId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        exportExamScoresUseCase = mock(ExportExamScoresUseCase.class);
        exportExamScoresExcelUseCase = mock(ExportExamScoresExcelUseCase.class);
        controller = new ExamResultController(
            mock(PreviewBulkFinalizeUseCase.class),
            mock(BulkFinalizeExamResultsUseCase.class),
            exportExamScoresUseCase,
            exportExamScoresExcelUseCase);

        when(exportExamScoresUseCase.execute(any())).thenReturn("Mã bài\r\n");
        when(exportExamScoresExcelUseCase.execute(any())).thenReturn(new byte[] {80, 75, 3, 4});
        when(exportExamScoresExcelUseCase.exportFileName(any()))
            .thenReturn("bang-diem-lop-20260806120000.xlsx");
    }

    private ExportExamScoresQuery capturedExcelQuery() {
        var captor = ArgumentCaptor.forClass(ExportExamScoresQuery.class);
        verify(exportExamScoresExcelUseCase).execute(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_forward_every_screen_filter_to_the_excel_use_case() {
        controller.exportScoresExcel(examId, scheduleId, teacherId, "RELEASED", "APPEAL",
            "COMPLETED", true, true, Boolean.TRUE, "nguyen", "CLASS_TEST");

        var query = capturedExcelQuery();
        assertThat(query.examId()).isEqualTo(examId);
        assertThat(query.scheduleId()).isEqualTo(scheduleId);
        assertThat(query.teacherId()).isEqualTo(teacherId);
        assertThat(query.resultStatus()).isEqualTo("RELEASED");
        assertThat(query.roundType()).isEqualTo("APPEAL");
        assertThat(query.assignmentStatus()).isEqualTo("COMPLETED");
        assertThat(query.unassignedOnly()).isTrue();
        assertThat(query.overdueOnly()).isTrue();
        assertThat(query.hasOpenAppeal()).isTrue();
        assertThat(query.keyword()).isEqualTo("nguyen");
        assertThat(query.examKind()).isEqualTo("CLASS_TEST");
    }

    /** {@code null} = không lọc, khác hẳn {@code false} = chỉ bài KHÔNG có đơn đang mở. */
    @Test
    void should_keep_an_absent_open_appeal_filter_null() {
        controller.exportScoresExcel(examId, null, null, null, null, null,
            false, false, null, null, null);

        assertThat(capturedExcelQuery().hasOpenAppeal()).isNull();
    }

    @Test
    void should_send_the_workbook_as_a_download_with_the_xlsx_content_type() {
        var response = controller.exportScoresExcel(examId, null, null, null, null, null,
            false, false, null, null, "CLASS_TEST");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
            .isEqualTo("attachment; filename=\"bang-diem-lop-20260806120000.xlsx\"");
        assertThat(response.getHeaders().getContentType())
            .hasToString("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        // Bốn byte đầu của mọi .xlsx là magic number của ZIP.
        assertThat(response.getBody()).startsWith((byte) 80, (byte) 75, (byte) 3, (byte) 4);
    }

    /** Tên file lấy theo loại kỳ thi, nên `kind` phải đi tới đúng chỗ đặt tên. */
    @Test
    void should_name_the_file_after_the_requested_kind() {
        controller.exportScoresExcel(examId, null, null, null, null, null,
            false, false, null, null, "CLASS_TEST");

        verify(exportExamScoresExcelUseCase).exportFileName("CLASS_TEST");
    }

    @Test
    void should_forward_the_same_filters_on_the_csv_route() {
        controller.exportScores(examId, null, teacherId, "RELEASED", "APPEAL", "COMPLETED",
            true, true, Boolean.FALSE, "nguyen", "CENTRALIZED");

        var captor = ArgumentCaptor.forClass(ExportExamScoresQuery.class);
        verify(exportExamScoresUseCase).execute(captor.capture());
        var query = captor.getValue();
        assertThat(query.examKind()).isEqualTo("CENTRALIZED");
        assertThat(query.hasOpenAppeal()).isFalse();
        assertThat(query.keyword()).isEqualTo("nguyen");
    }

    @Test
    void should_send_the_csv_as_utf8_so_vietnamese_survives() {
        var response = controller.exportScores(examId, null, null, null, null, null,
            false, false, null, null, null);

        assertThat(response.getHeaders().getContentType().getCharset())
            .isEqualTo(StandardCharsets.UTF_8);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
            .contains("bang-diem.csv");
    }
}
