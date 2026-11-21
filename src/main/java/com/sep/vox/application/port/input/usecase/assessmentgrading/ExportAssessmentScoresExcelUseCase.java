package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.mapper.assessmentgrading.GradingResultCode;
import com.sep.vox.application.port.input.query.ExportAssessmentScoresQuery;
import com.sep.vox.application.port.input.service.AssessmentScoreExportSupport;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.SpreadsheetWriterPort;
import com.sep.vox.application.query.dto.AssessmentScoreRowInfo;
import com.sep.vox.application.response.output.SpreadsheetCell;
import com.sep.vox.application.response.output.SpreadsheetDocument;
import com.sep.vox.application.response.output.SpreadsheetOptions;
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

    private static final String SHEET_NAME = "Bảng điểm";
    private static final String CELL_DATE_FORMAT = "HH:mm dd/MM/yyyy";
    private static final DateTimeFormatter FILE_STAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(DateMapper.DEFAULT_INPUT_ZONE);

    private static final List<String> HEADERS = List.of(
        "Mã bài", "Họ tên", "Email", "Lớp", "Kỳ thi", "Ca thi", "Điểm", "Xếp loại",
        "Trạng thái", "Vòng chấm cuối", "Kết luận", "Người chấm", "Thời điểm công bố");

    /**
     * Column width cap in characters. Auto-sizing shrinks to the longest cell, and one long assessment name is enough
     * to push a column off the screen and make the whole sheet unreadable.
     *
     * <p>The header stays visible when scrolling and every column has a filter arrow: the score sheet of one
     * assessment runs to hundreds of rows, and without these the reader loses the column names after the first screen.
     */
    private static final SpreadsheetOptions OPTIONS = new SpreadsheetOptions(true, true, true, 60, true);

    private final AssessmentScoreExportSupport assessmentScoreExportSupport;
    private final SpreadsheetWriterPort spreadsheetWriterPort;

    public ExportAssessmentScoresExcelUseCase(
            AssessmentScoreExportSupport assessmentScoreExportSupport,
            SpreadsheetWriterPort spreadsheetWriterPort) {
        this.assessmentScoreExportSupport = assessmentScoreExportSupport;
        this.spreadsheetWriterPort = spreadsheetWriterPort;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] execute(ExportAssessmentScoresQuery input) {
        List<List<SpreadsheetCell>> rows = assessmentScoreExportSupport.loadRows(input).stream()
            .map(this::toCells)
            .toList();
        return spreadsheetWriterPort.write(new SpreadsheetDocument(SHEET_NAME, HEADERS, rows, List.of(), OPTIONS));
    }

    /** ASCII-only file name, so {@code Content-Disposition} does not need RFC 5987 encoding. */
    public String exportFileName(AssessmentKind assessmentKind) {
        String scope = assessmentKind == AssessmentKind.CLASS_TEST ? "lop" : "tap-trung";
        return "bang-diem-" + scope + "-" + FILE_STAMP_FORMAT.format(Instant.now()) + ".xlsx";
    }

    private List<SpreadsheetCell> toCells(AssessmentScoreRowInfo info) {
        return List.of(
            text(info.candidateResultId() == null ? null : GradingResultCode.of(info.candidateResultId())),
            text(info.studentName()),
            text(info.studentEmail()),
            text(info.className()),
            text(info.assessmentName()),
            dateTime(info.scheduleStartAt()),
            SpreadsheetCell.number(info.totalScore()),
            text(info.resultBand()),
            text(info.status()),
            text(info.lastRoundType()),
            text(info.lastOutcome()),
            text(info.lastGraderName()),
            dateTime(info.releasedAt()));
    }

    private SpreadsheetCell text(String value) {
        return value == null || value.isEmpty() ? SpreadsheetCell.blank() : SpreadsheetCell.text(value);
    }

    /**
     * Writes a real date cell (not a string) after converting to Vietnam time: the container runs in UTC, so using
     * the {@code Instant} directly would be 7 hours off.
     */
    private SpreadsheetCell dateTime(Instant value) {
        return SpreadsheetCell.dateTime(
            value == null ? null : LocalDateTime.ofInstant(value, DateMapper.DEFAULT_INPUT_ZONE), CELL_DATE_FORMAT);
    }
}
