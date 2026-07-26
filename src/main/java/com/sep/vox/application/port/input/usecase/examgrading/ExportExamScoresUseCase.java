package com.sep.vox.application.port.input.usecase.examgrading;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        var rows = examGradingQueryRepository.findScoreRows(schoolId, input.examId(), input.scheduleId());
        var csv = new StringBuilder(UTF8_BOM).append(HEADER).append('\n');
        for (var row : rows) {
            csv.append(line(row)).append('\n');
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
            quote(row.scheduleName()),
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
     * Bọc mọi ô trong dấu nháy kép và nhân đôi nháy bên trong (RFC 4180). Tên học sinh
     * và lý do đều có thể chứa dấu phẩy hoặc xuống dòng; không bọc thì một cái tên là
     * đủ làm lệch toàn bộ cột của file.
     */
    private String quote(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
