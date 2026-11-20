package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.mapper.assessmentgrading.GradingResultCode;
import com.sep.vox.application.port.input.query.ExportAssessmentScoresQuery;
import com.sep.vox.application.port.input.service.AssessmentScoreExportSupport;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AssessmentScoreRowInfo;
import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * Exports the score sheet to Excel (.xlsx).
 *
 * <p>The same 13 columns and the same order as the CSV version ({@link ExportAssessmentScoresUseCase}) -- the two files
 * must be interchangeable, the user chooses the format and not the content. The difference
 * is in the CELL TYPE: scores are numeric cells and timestamps are date cells, so Excel can sort/filter/compute
 * averages -- something CSV, where everything is a string, cannot do.
 *
 * <p>Returns {@code byte[]} and not a file: the controller decides the file name and the
 * {@code Content-Disposition} header, the use case layer needs to know nothing about HTTP.
 */
@Service
public class ExportAssessmentScoresExcelUseCase implements IUseCase<ExportAssessmentScoresQuery, byte[]> {

    /** Ký tự mở đầu ô mà Excel/LibreOffice diễn giải thành công thức. */
    private static final String FORMULA_TRIGGERS = "=+-@\t\r";

    private static final String CELL_DATE_FORMAT = "HH:mm dd/MM/yyyy";
    private static final DateTimeFormatter FILE_STAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(DateMapper.DEFAULT_INPUT_ZONE);

    private static final List<String> HEADERS = List.of(
        "Mã bài", "Họ tên", "Email", "Lớp", "Kỳ thi", "Ca thi", "Điểm", "Xếp loại",
        "Trạng thái", "Vòng chấm cuối", "Kết luận", "Người chấm", "Thời điểm công bố");

    private static final int SITTING_TIME_COLUMN = 5;
    private static final int SCORE_COLUMN = 6;
    private static final int RELEASED_AT_COLUMN = 12;

    /**
     * Trần bề rộng cột. {@code autoSizeColumn} co theo ô dài nhất, và một tên kỳ thi dài
     * là đủ đẩy cột ra ngoài màn hình khiến cả bảng không đọc được.
     */
    private static final int MAX_COLUMN_WIDTH = 60 * 256;

    private final AssessmentScoreExportSupport assessmentScoreExportSupport;

    public ExportAssessmentScoresExcelUseCase(AssessmentScoreExportSupport assessmentScoreExportSupport) {
        this.assessmentScoreExportSupport = assessmentScoreExportSupport;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] execute(ExportAssessmentScoresQuery input) {
        List<AssessmentScoreRowInfo> rows = assessmentScoreExportSupport.loadRows(input);

        XSSFWorkbook workbook = new XSSFWorkbook();
        try (workbook) {
            XSSFSheet sheet = workbook.createSheet("Bảng điểm");
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle dateStyle = dateStyle(workbook);

            XSSFRow headerRow = sheet.createRow(0);
            for (int index = 0; index < HEADERS.size(); index++) {
                XSSFCell cell = headerRow.createCell(index);
                cell.setCellValue(HEADERS.get(index));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (AssessmentScoreRowInfo row : rows) {
                writeRow(sheet.createRow(rowIndex++), row, dateStyle);
            }

            // Header dính lại khi cuộn, và mỗi cột có sẵn mũi tên lọc: bảng điểm một kỳ thi
            // dài hàng trăm dòng, không có hai thứ này thì người đọc mất cột ngay màn hai.
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(rows.size(), 1), 0, HEADERS.size() - 1));
            autoSize(sheet);
            return toBytes(workbook);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo file export bảng điểm", exception);
        }
    }

    /** Tên file ASCII thuần — tránh phải encode {@code Content-Disposition} theo RFC 5987. */
    public String exportFileName(String assessmentKind) {
        String scope = AssessmentKind.CLASS_TEST.name().equals(assessmentKind) ? "lop" : "tap-trung";
        return "bang-diem-" + scope + "-" + FILE_STAMP_FORMAT.format(Instant.now()) + ".xlsx";
    }

    private void writeRow(Row row, AssessmentScoreRowInfo info, CellStyle dateStyle) {
        text(row, 0, info.candidateResultId() == null
            ? null : GradingResultCode.of(info.candidateResultId()));
        text(row, 1, info.studentName());
        text(row, 2, info.studentEmail());
        text(row, 3, info.className());
        text(row, 4, info.assessmentName());
        dateTime(row, SITTING_TIME_COLUMN, info.scheduleStartAt(), dateStyle);
        number(row, SCORE_COLUMN, info.totalScore());
        text(row, 7, info.resultBand());
        text(row, 8, info.status());
        text(row, 9, info.lastRoundType());
        text(row, 10, info.lastOutcome());
        text(row, 11, info.lastGraderName());
        dateTime(row, RELEASED_AT_COLUMN, info.releasedAt(), dateStyle);
    }

    /**
     * Ô mở đầu bằng {@code = + - @} (kể cả sau tab/CR) được prefix thêm một dấu nháy đơn:
     * đây là dữ liệu người dùng nhập, không phải công thức, nhưng Excel diễn giải nó là
     * công thức và CHẠY ngay khi mở file — {@code HYPERLINK}/{@code DDE} đủ để rò bảng
     * điểm ra ngoài. Người mở file là school admin, tài khoản quyền cao nhất trong trường.
     *
     * <p>Ghi ô .xlsx bỏ được BOM và quote của CSV, nhưng KHÔNG bỏ được cái này: rủi ro
     * nằm ở lúc Excel đọc ô, không nằm ở cách file được mã hoá.
     */
    private void text(Row row, int column, String value) {
        Cell cell = row.createCell(column);
        if (value == null || value.isEmpty()) {
            cell.setBlank();
            return;
        }
        cell.setCellValue(
            FORMULA_TRIGGERS.indexOf(value.charAt(0)) >= 0 ? "'" + value : value);
    }

    /** Ô trống chứ không phải {@code 0} khi chưa có điểm — 0 là một điểm số có thật. */
    private void number(Row row, int column, BigDecimal value) {
        Cell cell = row.createCell(column);
        if (value == null) {
            cell.setBlank();
            return;
        }
        cell.setCellValue(value.doubleValue());
    }

    /**
     * Ghi ô ngày thật (không phải chuỗi) sau khi đổi về giờ Việt Nam: container chạy UTC
     * nên lấy thẳng {@code Instant} sẽ lệch 7 tiếng.
     */
    private void dateTime(Row row, int column, Instant value, CellStyle dateStyle) {
        Cell cell = row.createCell(column);
        if (value == null) {
            cell.setBlank();
            return;
        }
        cell.setCellValue(LocalDateTime.ofInstant(value, DateMapper.DEFAULT_INPUT_ZONE));
        cell.setCellStyle(dateStyle);
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle dateStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(CELL_DATE_FORMAT));
        return style;
    }

    private void autoSize(Sheet sheet) {
        for (int index = 0; index < HEADERS.size(); index++) {
            sheet.autoSizeColumn(index);
            if (sheet.getColumnWidth(index) > MAX_COLUMN_WIDTH) {
                sheet.setColumnWidth(index, MAX_COLUMN_WIDTH);
            }
        }
    }

    private byte[] toBytes(XSSFWorkbook workbook) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
