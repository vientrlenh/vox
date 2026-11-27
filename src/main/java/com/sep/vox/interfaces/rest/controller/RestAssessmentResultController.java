package com.sep.vox.interfaces.rest.controller;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.assessmentgrading.BulkFinalizeAssessmentResultsCommand;
import com.sep.vox.application.port.input.query.assessmentgrading.ExportAssessmentScoresQuery;
import com.sep.vox.application.port.input.usecase.assessmentgrading.BulkFinalizeAssessmentResultsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ExportAssessmentScoresExcelUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ExportAssessmentScoresUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.PreviewBulkFinalizeUseCase;
import com.sep.vox.application.query.dto.BulkFinalizePreviewInfo;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.interfaces.rest.dto.request.assessmentgrading.BulkFinalizeAssessmentResultsRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

/**
 * Assessment-level actions of the school admin: finalize the results and export the score sheet.
 *
 * <p>The old {@code /{id}/review} endpoint was removed -- the verdict on a suspicious
 * submission is now the decision of the assigned teacher, made through
 * {@code /api/v1/grading-assignments/{id}/invalidate}.
 */
@RestController
@RequestMapping("/api/v1/assessment-results")
@RequiredArgsConstructor
public class RestAssessmentResultController {

    /**
     * MIME type of .xlsx. Written out as a constant because the string is long, and one wrong character
     * makes the browser download an Excel file that cannot be opened.
     */
    private static final String XLSX_MEDIA_TYPE =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final PreviewBulkFinalizeUseCase previewBulkFinalizeUseCase;
    private final BulkFinalizeAssessmentResultsUseCase bulkFinalizeAssessmentResultsUseCase;
    private final ExportAssessmentScoresUseCase exportAssessmentScoresUseCase;
    private final ExportAssessmentScoresExcelUseCase exportAssessmentScoresExcelUseCase;

    @Operation(summary = "Previews the finalization of an assessment: how many results are still ungraded, "
        + "half-graded, and how many appeals are unresolved. Writes NOTHING.")
    @GetMapping("/finalize/preview")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<BulkFinalizePreviewInfo>> previewFinalize(
            @RequestParam("assessmentId") UUID assessmentId) {
        return ResponseEntity.ok(
            ApiResponse.success("Finalization info retrieved successfully!", previewBulkFinalizeUseCase.execute(assessmentId)));
    }

    @Operation(summary = "Finalizes an assessment: releases (RELEASED) the results still waiting for a grader using the current AI score, "
        + "so the assessment can be published. If half-graded results remain, `releasePendingWithAiScores` must be set "
        + "to confirm. Results under appeal block it; RELEASED/INVALID results are left unchanged.")
    @PostMapping("/finalize")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Integer>> finalizeResults(
            @Valid @RequestBody BulkFinalizeAssessmentResultsRequest request) {
        BulkFinalizeAssessmentResultsCommand command = new BulkFinalizeAssessmentResultsCommand(
            request.assessmentId(), Boolean.TRUE.equals(request.releasePendingWithAiScores()));
        return ResponseEntity.ok(
            ApiResponse.success("Assessment finalized successfully!", bulkFinalizeAssessmentResultsUseCase.execute(command)));
    }

    @Operation(summary = "Exports the score sheet to CSV (UTF-8 with BOM so that Excel reads Vietnamese correctly). "
        + "At least one of `assessmentId` / `scheduleId` must be passed. The filter is identical to the "
        + "dispatch board; an empty `kind` is read as `CENTRALIZED`.")
    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<byte[]> exportScores(
            @RequestParam(name = "assessmentId", required = false) UUID assessmentId,
            @RequestParam(name = "scheduleId", required = false) UUID scheduleId,
            @RequestParam(name = "teacherId", required = false) UUID teacherId,
            @RequestParam(name = "resultStatus", required = false) AssessmentCandidateResultStatus resultStatus,
            @RequestParam(name = "roundType", required = false) GradingRoundType roundType,
            @RequestParam(name = "assignmentStatus", required = false) GradingAssignmentStatus assignmentStatus,
            @RequestParam(name = "unassignedOnly", required = false, defaultValue = "false") boolean unassignedOnly,
            @RequestParam(name = "overdueOnly", required = false, defaultValue = "false") boolean overdueOnly,
            @RequestParam(name = "hasOpenAppeal", required = false) Boolean hasOpenAppeal,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "kind", required = false) AssessmentKind kind) {
        ExportAssessmentScoresQuery query = toQuery(assessmentId, scheduleId, teacherId, resultStatus, roundType, assignmentStatus,
            unassignedOnly, overdueOnly, hasOpenAppeal, keyword, kind);
        String csv = exportAssessmentScoresUseCase.execute(query);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bang-diem.csv\"")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Operation(summary = "Exports the score sheet to Excel (.xlsx): the same 13 columns as the CSV, but scores are numeric cells "
        + "and timestamps are date cells, so they can be sorted, filtered and calculated. At least one of "
        + "`assessmentId` / `scheduleId` must be passed. The filter is identical to the dispatch board; an empty "
        + "`kind` is read as `CENTRALIZED`.")
    @GetMapping(value = "/export/excel", produces = XLSX_MEDIA_TYPE)
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<byte[]> exportScoresExcel(
            @RequestParam(name = "assessmentId", required = false) UUID assessmentId,
            @RequestParam(name = "scheduleId", required = false) UUID scheduleId,
            @RequestParam(name = "teacherId", required = false) UUID teacherId,
            @RequestParam(name = "resultStatus", required = false) AssessmentCandidateResultStatus resultStatus,
            @RequestParam(name = "roundType", required = false) GradingRoundType roundType,
            @RequestParam(name = "assignmentStatus", required = false) GradingAssignmentStatus assignmentStatus,
            @RequestParam(name = "unassignedOnly", required = false, defaultValue = "false") boolean unassignedOnly,
            @RequestParam(name = "overdueOnly", required = false, defaultValue = "false") boolean overdueOnly,
            @RequestParam(name = "hasOpenAppeal", required = false) Boolean hasOpenAppeal,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "kind", required = false) AssessmentKind kind) {
        ExportAssessmentScoresQuery query = toQuery(assessmentId, scheduleId, teacherId, resultStatus, roundType, assignmentStatus,
            unassignedOnly, overdueOnly, hasOpenAppeal, keyword, kind);
        byte[] workbook = exportAssessmentScoresExcelUseCase.execute(query);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + exportAssessmentScoresExcelUseCase.exportFileName(kind) + "\"")
            .contentType(MediaType.parseMediaType(XLSX_MEDIA_TYPE))
            .body(workbook);
    }

    /**
     * The two export endpoints differ only in how the file is written, so the filter is built in one place:
     * writing it twice invites the CSV and the Excel export of the same screen to return two different sets of rows.
     */
    private ExportAssessmentScoresQuery toQuery(
            UUID assessmentId, UUID scheduleId, UUID teacherId, AssessmentCandidateResultStatus resultStatus,
            GradingRoundType roundType, GradingAssignmentStatus assignmentStatus, boolean unassignedOnly, boolean overdueOnly,
            Boolean hasOpenAppeal, String keyword, AssessmentKind kind) {
        return new ExportAssessmentScoresQuery(assessmentId, scheduleId, teacherId, resultStatus, roundType,
            assignmentStatus, unassignedOnly, overdueOnly, hasOpenAppeal, keyword, kind);
    }
}
