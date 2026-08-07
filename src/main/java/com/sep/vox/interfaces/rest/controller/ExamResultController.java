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
import com.sep.vox.application.port.input.usecase.examgrading.ExportExamScoresExcelUseCase;
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

    /**
     * MIME type của .xlsx. Viết ra hằng vì chuỗi này dài và sai một ký tự thì trình duyệt
     * tải về một file Excel không mở được.
     */
    private static final String XLSX_MEDIA_TYPE =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final PreviewBulkFinalizeUseCase previewBulkFinalizeUseCase;
    private final BulkFinalizeExamResultsUseCase bulkFinalizeExamResultsUseCase;
    private final ExportExamScoresUseCase exportExamScoresUseCase;
    private final ExportExamScoresExcelUseCase exportExamScoresExcelUseCase;

    public ExamResultController(
            PreviewBulkFinalizeUseCase previewBulkFinalizeUseCase,
            BulkFinalizeExamResultsUseCase bulkFinalizeExamResultsUseCase,
            ExportExamScoresUseCase exportExamScoresUseCase,
            ExportExamScoresExcelUseCase exportExamScoresExcelUseCase) {
        this.previewBulkFinalizeUseCase = previewBulkFinalizeUseCase;
        this.bulkFinalizeExamResultsUseCase = bulkFinalizeExamResultsUseCase;
        this.exportExamScoresUseCase = exportExamScoresUseCase;
        this.exportExamScoresExcelUseCase = exportExamScoresExcelUseCase;
    }

    @Operation(summary = "Xem trước việc chốt sổ kỳ thi: còn bao nhiêu bài chưa chấm, "
        + "đang chấm dở, và đơn phúc khảo chưa xong. KHÔNG ghi gì.")
    @GetMapping("/finalize/preview")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<BulkFinalizePreviewInfo>> previewFinalize(
            @RequestParam("examId") UUID examId) {
        return ResponseEntity.ok(
            ApiResponse.success("Lấy thông tin chốt sổ thành công!", previewBulkFinalizeUseCase.execute(examId)));
    }

    @Operation(summary = "Chốt sổ kỳ thi: công bố (RELEASED) các bài còn chờ người chấm theo điểm AI "
        + "đang có, để kỳ thi publish được. Nếu còn bài dở, phải bật `releasePendingWithAiScores` "
        + "để xác nhận. Bài đang phúc khảo chặn cứng; bài RELEASED/INVALID giữ nguyên.")
    @PostMapping("/finalize")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Integer>> finalizeResults(
            @Valid @RequestBody BulkFinalizeExamResultsRequest request) {
        var command = new BulkFinalizeExamResultsCommand(
            request.examId(), Boolean.TRUE.equals(request.releasePendingWithAiScores()));
        return ResponseEntity.ok(
            ApiResponse.success("Chốt sổ kỳ thi thành công!", bulkFinalizeExamResultsUseCase.execute(command)));
    }

    @Operation(summary = "Xuất bảng điểm ra CSV (UTF-8 có BOM để Excel đọc đúng tiếng Việt). "
        + "Phải truyền ít nhất một trong `examId` / `scheduleId`. Bộ lọc nhận vào giống hệt "
        + "bảng điều phối, `kind` bỏ trống được hiểu là `CENTRALIZED`.")
    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<byte[]> exportScores(
            @RequestParam(name = "examId", required = false) UUID examId,
            @RequestParam(name = "scheduleId", required = false) UUID scheduleId,
            @RequestParam(name = "teacherId", required = false) UUID teacherId,
            @RequestParam(name = "resultStatus", required = false) String resultStatus,
            @RequestParam(name = "roundType", required = false) String roundType,
            @RequestParam(name = "assignmentStatus", required = false) String assignmentStatus,
            @RequestParam(name = "unassignedOnly", required = false, defaultValue = "false") boolean unassignedOnly,
            @RequestParam(name = "overdueOnly", required = false, defaultValue = "false") boolean overdueOnly,
            @RequestParam(name = "hasOpenAppeal", required = false) Boolean hasOpenAppeal,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "kind", required = false) String kind) {
        var query = toQuery(examId, scheduleId, teacherId, resultStatus, roundType, assignmentStatus,
            unassignedOnly, overdueOnly, hasOpenAppeal, keyword, kind);
        var csv = exportExamScoresUseCase.execute(query);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bang-diem.csv\"")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Operation(summary = "Xuất bảng điểm ra Excel (.xlsx): cùng 13 cột với bản CSV nhưng điểm là ô "
        + "số và mốc thời gian là ô ngày, nên sort/lọc/tính được. Phải truyền ít nhất một trong "
        + "`examId` / `scheduleId`. Bộ lọc nhận vào giống hệt bảng điều phối, `kind` bỏ trống "
        + "được hiểu là `CENTRALIZED`.")
    @GetMapping(value = "/export/excel", produces = XLSX_MEDIA_TYPE)
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<byte[]> exportScoresExcel(
            @RequestParam(name = "examId", required = false) UUID examId,
            @RequestParam(name = "scheduleId", required = false) UUID scheduleId,
            @RequestParam(name = "teacherId", required = false) UUID teacherId,
            @RequestParam(name = "resultStatus", required = false) String resultStatus,
            @RequestParam(name = "roundType", required = false) String roundType,
            @RequestParam(name = "assignmentStatus", required = false) String assignmentStatus,
            @RequestParam(name = "unassignedOnly", required = false, defaultValue = "false") boolean unassignedOnly,
            @RequestParam(name = "overdueOnly", required = false, defaultValue = "false") boolean overdueOnly,
            @RequestParam(name = "hasOpenAppeal", required = false) Boolean hasOpenAppeal,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "kind", required = false) String kind) {
        var query = toQuery(examId, scheduleId, teacherId, resultStatus, roundType, assignmentStatus,
            unassignedOnly, overdueOnly, hasOpenAppeal, keyword, kind);
        var workbook = exportExamScoresExcelUseCase.execute(query);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + exportExamScoresExcelUseCase.exportFileName(kind) + "\"")
            .contentType(MediaType.parseMediaType(XLSX_MEDIA_TYPE))
            .body(workbook);
    }

    /**
     * Hai endpoint xuất chỉ khác nhau ở cách ghi file, nên bộ lọc phải dựng ở một chỗ:
     * viết hai lần là mở đường cho CSV và Excel cùng một màn hình trả về hai tập dòng khác nhau.
     */
    private ExportExamScoresQuery toQuery(
            UUID examId, UUID scheduleId, UUID teacherId, String resultStatus, String roundType,
            String assignmentStatus, boolean unassignedOnly, boolean overdueOnly,
            Boolean hasOpenAppeal, String keyword, String kind) {
        return new ExportExamScoresQuery(examId, scheduleId, teacherId, resultStatus, roundType,
            assignmentStatus, unassignedOnly, overdueOnly, hasOpenAppeal, keyword, kind);
    }
}
