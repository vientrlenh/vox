package com.sep.vox.application.response.input.dashboard;

import java.util.List;
import java.util.UUID;

/**
 * Ai đã tiêu bao nhiêu hạn mức AI của trường trong cửa sổ đang xem.
 *
 * <p>{@code schoolWideCostVnd} là phần KHÔNG thuộc về ai — kỳ thi tập trung do nhà trường tổ chức nên
 * cố ý không tính vào trần chi của người nào. Trả riêng thay vì nhét thành một hàng "không rõ": ở
 * gần như mọi trường nó là khoản lớn nhất và sẽ chiếm đỉnh bảng xếp hạng, che mất đúng thứ bảng này
 * sinh ra để cho thấy.
 *
 * <p>Không có nó thì tổng của bảng không bao giờ khớp tổng của biểu đồ ngay bên trên, và người đọc
 * sẽ đi tìm một khoản thất thoát không tồn tại.
 */
public record SchoolAiSpendByUserPageResponse(
    List<UserAiSpendResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    String schoolWideCostVnd
) {

    public record UserAiSpendResponse(
        UUID userId,
        String fullName,
        String quotaType,
        String spentVnd,
        /** Trần chi cá nhân; null khi nhà trường chưa chia trần cho người này. */
        String allocatedAmountVnd
    ) {
    }
}
