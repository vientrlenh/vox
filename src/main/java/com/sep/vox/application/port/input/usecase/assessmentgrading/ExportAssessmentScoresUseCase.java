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
        "Result code", "Full name", "Email", "Class", "Assessment", "Schedule", "Score", "Result band",
        "Status", "Last grading round", "Conclusion", "Grader", "Released at");

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
     * EVERY timestamp in the file goes through here: converted to Vietnam time before printing.
     * The container runs UTC so formatting directly would be off by 7 hours -- the same bug as the two mail
     * listeners. No raw ISO variant is kept: two time columns in two formats in the same
     * file is something the reader has no way to notice by themselves.
     */
    private String localTime(Instant value) {
        return value == null ? "" : LOCAL_TIME_FORMAT.format(value);
    }

    /**
     * Wraps every cell in double quotes and doubles the quotes inside (RFC 4180). Student names
     * and reasons can contain a comma or a line break; without wrapping, a single name is
     * enough to shift all the columns of the file.
     *
     * <p>A cell starting with {@code = + - @} (even after a tab/CR) gets an extra single quote
     * prefix: this is user-entered data, not a formula, but Excel interprets
     * it as a formula and RUNS it when the file is opened -- {@code HYPERLINK}/{@code DDE} is enough to leak
     * the score sheet. The person opening the file is a school admin, the highest-privilege account in the
     * school.
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
