package com.sep.vox.application.response.input.importfile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kết quả bước xem trước của MỌI luồng import: id phiên, tiêu đề cột gốc, gợi ý ghép cột và vài
 * dòng mẫu để người dùng đối chiếu trước khi xác nhận.
 *
 * <p>Không mang gì riêng của một loại dữ liệu nào. {@code PreviewQuestionImportResponse} có nội
 * dung y hệt nhưng giữ nguyên, không gộp — đổi kiểu trả về của một endpoint đang chạy là rủi ro
 * không đáng cho một lần dọn tên.
 */
public record PreviewImportResponse(
    UUID importSessionId,
    String fileName,
    List<String> originalHeaders,
    Map<String, String> suggestedMapping,
    List<Map<String, String>> sampleRows,
    long totalRows,
    String expiresAt
) {
}
