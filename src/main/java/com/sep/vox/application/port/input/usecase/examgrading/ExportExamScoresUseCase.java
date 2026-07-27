package com.sep.vox.application.port.input.usecase.examgrading;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.query.ExportExamScoresQuery;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.ExamScoreRowInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;

/**
 * Xuất bảng điểm kỳ thi ra CSV.
 *
 * <p>Trả về chuỗi CSV chứ không phải file: controller quyết tên file và header
 * {@code Content-Disposition}, tầng use case không cần biết gì về HTTP.
 *
 * <p>Mở đầu bằng BOM UTF-8 — Excel bản Windows đọc CSV không BOM theo codepage hệ
 * thống và làm hỏng toàn bộ tiếng Việt có dấu, đây là lý do duy nhất BOM có mặt ở đây.
 */
@Service
public class ExportExamScoresUseCase implements IUseCase<ExportExamScoresQuery, String> {

    private static final String UTF8_BOM = "﻿";
    /** RFC 4180 quy định CRLF, và Excel bản Windows là nơi file này thực sự được mở. */
    private static final String LINE_BREAK = "\r\n";
    /** Ký tự mở đầu ô mà Excel/LibreOffice diễn giải thành công thức. */
    private static final String FORMULA_TRIGGERS = "=+-@\t\r";
    private static final DateTimeFormatter LOCAL_TIME_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(DateMapper.APP_ZONE);
    private static final String HEADER = String.join(",",
        "Mã bài", "Họ tên", "Email", "Lớp", "Kỳ thi", "Ca thi", "Điểm", "Xếp loại",
        "Trạng thái", "Vòng chấm cuối", "Kết luận", "Người chấm", "Thời điểm công bố");

    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public ExportExamScoresUseCase(
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public String execute(ExportExamScoresQuery input) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var schoolId = examGradingAccessService.requireCurrentSchoolId(currentUserId);
        examGradingAccessService.authorizeSchoolAdmin(schoolId, currentUserId);

        // Không phạm vi = xuất mọi kỳ thi của cả trường: hàng chục nghìn dòng dựng
        // trong RAM, kèm ba query không phân trang với mệnh đề IN khổng lồ. Bắt chọn
        // phạm vi, cùng khuôn với AutoAssignGradingUseCase.
        if (input.examId() == null && input.scheduleId() == null) {
            throw new IllegalArgumentException("Phải chọn kỳ thi hoặc ca thi để xuất bảng điểm.");
        }

        var rows = examGradingQueryRepository.findScoreRows(schoolId, input.examId(), input.scheduleId());
        var csv = new StringBuilder(UTF8_BOM).append(HEADER).append(LINE_BREAK);
        for (var row : rows) {
            csv.append(line(row)).append(LINE_BREAK);
        }
        return csv.toString();
    }

    private String line(ExamScoreRowInfo row) {
        return String.join(",",
            quote(shortCode(row)),
            quote(row.studentName()),
            quote(row.studentEmail()),
            quote(row.className()),
            quote(row.examName()),
            quote(localTime(row.scheduleStartAt())),
            quote(number(row.totalScore())),
            quote(row.resultBand()),
            quote(row.status()),
            quote(row.lastRoundType()),
            quote(row.lastOutcome()),
            quote(row.lastGraderName()),
            quote(time(row.releasedAt())));
    }

    private String shortCode(ExamScoreRowInfo row) {
        return row.candidateResultId() == null ? "" : com.sep.vox.application.mapper.examgrading.GradingResultCode
            .of(row.candidateResultId());
    }

    private String number(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private String time(OffsetDateTime value) {
        return value == null ? "" : value.toString();
    }

    /**
     * Mốc thời gian người đọc: đổi về giờ Việt Nam trước khi in. Container chạy UTC nên
     * format thẳng sẽ lệch 7 tiếng — cùng lỗi với hai listener mail.
     */
    private String localTime(OffsetDateTime value) {
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
        var safe = value;
        if (!safe.isEmpty() && FORMULA_TRIGGERS.indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
