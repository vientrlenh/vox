package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

/**
 * Giáo viên tạo bài kiểm tra trên lớp tự nhận chấm một loạt bài.
 *
 * <p>Vòng {@code INITIAL} thường đã được mở tự động; lệnh này dành cho hai vòng còn
 * lại ({@code SPOT_CHECK} soi lại bài đã công bố, {@code REMEDIATION} soi lại bài bị
 * vô hiệu), và cho trường hợp bài {@code INITIAL} chưa có phân công vì lý do nào đó.
 */
public record ClaimClassTestGradingCommand(
    UUID examId,
    String roundType,
    List<UUID> candidateResultIds
) {
}
