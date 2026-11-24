package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.query.assessmentgrading.ExportAssessmentScoresQuery;
import com.sep.vox.application.port.input.service.AssessmentScoreExportSupport;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AssessmentScoreRowInfo;

/**
 * Exports the assessment score sheet to CSV.
 *
 * <p>Returns a CSV string and not a file: the controller decides the file name and the
 * {@code Content-Disposition} header, the use case layer needs to know nothing about HTTP.
 *
 * <p>Starts with a UTF-8 BOM -- Excel on Windows reads a CSV without a BOM using the system
 * codepage and corrupts all accented Vietnamese text; this is the only reason the BOM is here.
 *
 * <p>Permission checking, scope fixing and row loading live in {@link AssessmentScoreExportSupport} -- shared
 * with the Excel export, so the two formats cannot diverge on what gets exported.
 */
@Service
@RequiredArgsConstructor
public class ExportAssessmentScoresUseCase implements IUseCase<ExportAssessmentScoresQuery, String> {

    private static final String UTF8_BOM = "﻿";
    /** RFC 4180 specifies CRLF, and Excel on Windows is where this file is actually opened. */
    private static final String LINE_BREAK = "\r\n";
    /** Leading cell characters that Excel/LibreOffice interpret as a formula. */
    private static final String FORMULA_TRIGGERS = "=+-@\t\r";
    private static final DateTimeFormatter LOCAL_TIME_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(DateMapper.DEFAULT_INPUT_ZONE);
    private static final String HEADER = String.join(",",
        "Mã bài", "Họ tên", "Email", "Lớp", "Kỳ thi", "Ca thi", "Điểm", "Xếp loại",
        "Trạng thái", "Vòng chấm cuối", "Kết luận", "Người chấm", "Thời điểm công bố");

    private final AssessmentScoreExportSupport assessmentScoreExportSupport;

    @Override
    @Transactional(readOnly = true)
    public String execute(ExportAssessmentScoresQuery input) {
        List<AssessmentScoreRowInfo> rows = assessmentScoreExportSupport.loadRows(input);
        StringBuilder csv = new StringBuilder(UTF8_BOM).append(HEADER).append(LINE_BREAK);
        for (AssessmentScoreRowInfo row : rows) {
            csv.append(line(row)).append(LINE_BREAK);
        }
        return csv.toString();
    }

    private String line(AssessmentScoreRowInfo row) {
        return String.join(",",
            quote(shortCode(row)),
            quote(row.studentName()),
            quote(row.studentEmail()),
            quote(row.className()),
            quote(row.assessmentName()),
            quote(localTime(row.scheduleStartAt())),
            quote(number(row.totalScore())),
            quote(row.resultBand()),
            quote(row.status()),
            quote(row.lastRoundType()),
            quote(row.lastOutcome()),
            quote(row.lastGraderName()),
            quote(localTime(row.releasedAt())));
    }

    private String shortCode(AssessmentScoreRowInfo row) {
        return row.candidateResultId() == null ? "" : com.sep.vox.application.mapper.assessmentgrading.GradingResultCode
            .of(row.candidateResultId());
    }

    private String number(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    /**
     * MỌI mốc thời gian trong file đều đi qua đây: đổi về giờ Việt Nam trước khi in.
     * Container chạy UTC nên format thẳng sẽ lệch 7 tiếng — cùng lỗi với hai listener
     * mail. Không giữ biến thể in ISO thô: hai cột thời gian hai định dạng trong cùng
     * một file là thứ người đọc không có cách nào tự nhận ra.
     */
    private String localTime(Instant value) {
        return value == null ? "" : LOCAL_TIME_FORMAT.format(value);
    }

    /**
     * Bọc mọi ô trong dấu nháy kép và nhân đôi nháy bên trong (RFC 4180). Tên học sinh
     * và lý do đều có thể chứa dấu phẩy hoặc xuống dòng; không bọc thì một cái tên là
     * đủ làm lệch toàn bộ cột của file.
     *
     * <p>Ô mở đầu bằng {@code = + - @} (kể cả sau tab/CR) được prefix thêm một dấu nháy
     * đơn: đây là dữ liệu người dùng nhập, không phải công thức, nhưng Excel diễn giải
     * nó là công thức và CHẠY ngay khi mở file — {@code HYPERLINK}/{@code DDE} đủ để rò
     * bảng điểm ra ngoài. Người mở file là school admin, tài khoản quyền cao nhất trong
     * trường.
     */
    private String quote(String value) {
        if (value == null) {
            return "\"\"";
        }
        String safe = value;
        if (!safe.isEmpty() && FORMULA_TRIGGERS.indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
