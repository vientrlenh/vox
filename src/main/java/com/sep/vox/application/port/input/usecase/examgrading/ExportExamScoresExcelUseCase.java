package com.sep.vox.application.port.input.usecase.examgrading;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.mapper.examgrading.GradingResultCode;
import com.sep.vox.application.port.input.query.ExportExamScoresQuery;
import com.sep.vox.application.port.input.service.ExamScoreExportSupport;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.ExamScoreRowInfo;
import com.sep.vox.domain.model.exam.ExamKind;

/**
 * Xuất bảng điểm ra Excel (.xlsx).
 *
 * <p>Cùng 13 cột và cùng thứ tự với bản CSV ({@link ExportExamScoresUseCase}) — hai file
 * phải thay thế được cho nhau, người dùng chọn định dạng chứ không chọn nội dung. Khác
 * biệt nằm ở KIỂU Ô: điểm là ô số và mốc thời gian là ô ngày, nên Excel sort/lọc/tính
 * trung bình được — thứ mà CSV, nơi mọi thứ đều là chuỗi, không làm được.
 *
 * <p>Trả về {@code byte[]} chứ không phải file: controller quyết tên file và header
 * {@code Content-Disposition}, tầng use case không cần biết gì về HTTP.
 */
@Service
public class ExportExamScoresExcelUseCase implements IUseCase<ExportExamScoresQuery, byte[]> {

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

    private final ExamScoreExportSupport examScoreExportSupport;

    public ExportExamScoresExcelUseCase(ExamScoreExportSupport examScoreExportSupport) {
        this.examScoreExportSupport = examScoreExportSupport;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] execute(ExportExamScoresQuery input) {
        var rows = examScoreExportSupport.loadRows(input);

        var workbook = new XSSFWorkbook();
        try (workbook) {
            var sheet = workbook.createSheet("Bảng điểm");
            var headerStyle = headerStyle(workbook);
            var dateStyle = dateStyle(workbook);

            var headerRow = sheet.createRow(0);
            for (var index = 0; index < HEADERS.size(); index++) {
                var cell = headerRow.createCell(index);
                cell.setCellValue(HEADERS.get(index));
                cell.setCellStyle(headerStyle);
            }

            var rowIndex = 1;
            for (var row : rows) {
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
    public String exportFileName(String examKind) {
        var scope = ExamKind.CLASS_TEST.name().equals(examKind) ? "lop" : "tap-trung";
        return "bang-diem-" + scope + "-" + FILE_STAMP_FORMAT.format(Instant.now()) + ".xlsx";
    }

    private void writeRow(Row row, ExamScoreRowInfo info, CellStyle dateStyle) {
        text(row, 0, info.candidateResultId() == null
            ? null : GradingResultCode.of(info.candidateResultId()));
        text(row, 1, info.studentName());
        text(row, 2, info.studentEmail());
        text(row, 3, info.className());
        text(row, 4, info.examName());
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
        var cell = row.createCell(column);
        if (value == null || value.isEmpty()) {
            cell.setBlank();
            return;
        }
        cell.setCellValue(
            FORMULA_TRIGGERS.indexOf(value.charAt(0)) >= 0 ? "'" + value : value);
    }

    /** Ô trống chứ không phải {@code 0} khi chưa có điểm — 0 là một điểm số có thật. */
    private void number(Row row, int column, BigDecimal value) {
        var cell = row.createCell(column);
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
        var cell = row.createCell(column);
        if (value == null) {
            cell.setBlank();
            return;
        }
        cell.setCellValue(LocalDateTime.ofInstant(value, DateMapper.DEFAULT_INPUT_ZONE));
        cell.setCellStyle(dateStyle);
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        var font = workbook.createFont();
        font.setBold(true);
        var style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle dateStyle(XSSFWorkbook workbook) {
        var style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(CELL_DATE_FORMAT));
        return style;
    }

    private void autoSize(Sheet sheet) {
        for (var index = 0; index < HEADERS.size(); index++) {
            sheet.autoSizeColumn(index);
            if (sheet.getColumnWidth(index) > MAX_COLUMN_WIDTH) {
                sheet.setColumnWidth(index, MAX_COLUMN_WIDTH);
            }
        }
    }

    private byte[] toBytes(XSSFWorkbook workbook) throws IOException {
        try (var outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
