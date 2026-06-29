package com.sep.vox.application.response.input.importfile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PreviewRubricResultBandImportResponse(
        UUID sessionId,                          // ID của phiên làm việc import vừa tạo
        String fileName,                         // Tên file Excel/CSV
        List<String> originalHeaders,            // Danh sách các tiêu đề cột thô đọc được từ file
        Map<String, String> suggestedMapping,    // Gợi ý cấu hình khớp cột (Cột Excel -> Field hệ thống)
        List<Map<String, String>> sampleRows,    // Data xem trước của vài dòng đầu (Để UI hiển thị bảng Preview)
        long totalRows,                          // Tổng số dòng dữ liệu đọc được
        OffsetDateTime expiresAt                 // Hạn sử dụng của Session này
) {
}