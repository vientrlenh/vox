package com.sep.vox.application.response.input.question;

import java.util.UUID;

/**
 * Một mục bị bỏ qua trong lần đổi trạng thái hàng loạt.
 *
 * <p>Kèm {@code code} và {@code currentStatus} để client hiện được "mục nào, đang ở đâu, vì sao"
 * mà không phải tra ngược sang danh sách đang xem -- mục không nằm trên trang hiện tại vẫn hiển thị
 * đủ thông tin. {@code reasonCode} để gom nhóm theo lý do.
 *
 * @param code mã của ngân hàng/chủ đề, {@code null} khi không tìm thấy
 * @param currentStatus tên enum trạng thái, {@code null} khi không tìm thấy
 */
public record BulkUpdateQuestionScopeStatusFailure(
    UUID id,
    String code,
    String currentStatus,
    String reasonCode,
    String reason
) {
}
