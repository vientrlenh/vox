package com.sep.vox.interfaces.rest.controller;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

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

import com.sep.vox.application.port.input.command.BulkFinalizeExamResultsCommand;
import com.sep.vox.application.port.input.query.ExportExamScoresQuery;
import com.sep.vox.application.port.input.usecase.examgrading.BulkFinalizeExamResultsUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ExportExamScoresUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.PreviewBulkFinalizeUseCase;
import com.sep.vox.application.query.dto.BulkFinalizePreviewInfo;
import com.sep.vox.interfaces.rest.dto.request.BulkFinalizeExamResultsRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

/**
 * Thao tác cấp kỳ thi của school admin: chốt sổ và xuất bảng điểm.
 *
 * <p>Endpoint {@code /{id}/review} cũ đã bị gỡ — kết luận bài nghi vấn nay là quyết
 * định của giáo viên được giao, qua
 * {@code /api/v1/grading-assignments/{id}/invalidate}.
 */
@RestController
@RequestMapping("/api/v1/exam-results")
public class ExamResultController {

    private final PreviewBulkFinalizeUseCase previewBulkFinalizeUseCase;
    private final BulkFinalizeExamResultsUseCase bulkFinalizeExamResultsUseCase;
    private final ExportExamScoresUseCase exportExamScoresUseCase;

    public ExamResultController(
            PreviewBulkFinalizeUseCase previewBulkFinalizeUseCase,
            BulkFinalizeExamResultsUseCase bulkFinalizeExamResultsUseCase,
            ExportExamScoresUseCase exportExamScoresUseCase) {
        this.previewBulkFinalizeUseCase = previewBulkFinalizeUseCase;
        this.bulkFinalizeExamResultsUseCase = bulkFinalizeExamResultsUseCase;
        this.exportExamScoresUseCase = exportExamScoresUseCase;
    }

    @Operation(summary = "Xem trước việc chốt sổ kỳ thi: còn bao nhiêu bài chưa chấm, "
        + "đang chấm dở, và đơn phúc khảo chưa xong. KHÔNG ghi gì.")
    @GetMapping("/finalize/preview")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<BulkFinalizePreviewInfo>> previewFinalize(
            @RequestParam("examId") UUID examId) {
        return ResponseEntity.ok(
            ApiResponse.success("Lấy thông tin chốt sổ thành công!", previewBulkFinalizeUseCase.execute(examId)));
    }

    @Operation(summary = "Chốt sổ toàn bộ kết quả của kỳ thi về FINAL. Nếu còn bài dở, phải bật "
        + "`releasePendingWithAiScores` để xác nhận công bố theo điểm AI hiện có.")
    @PostMapping("/finalize")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> finalizeResults(
            @Valid @RequestBody BulkFinalizeExamResultsRequest request) {
        var command = new BulkFinalizeExamResultsCommand(
            request.examId(), Boolean.TRUE.equals(request.releasePendingWithAiScores()));
        return ResponseEntity.ok(
            ApiResponse.success("Chốt sổ kỳ thi thành công!", bulkFinalizeExamResultsUseCase.execute(command)));
    }

    @Operation(summary = "Xuất bảng điểm ra CSV (UTF-8 có BOM để Excel đọc đúng tiếng Việt)")
    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<byte[]> exportScores(
            @RequestParam(value = "examId", required = false) UUID examId,
            @RequestParam(value = "scheduleId", required = false) UUID scheduleId) {
        var csv = exportExamScoresUseCase.execute(new ExportExamScoresQuery(examId, scheduleId));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bang-diem.csv\"")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
