package com.sep.vox.application.response.input.examgrading;

import java.util.List;
import java.util.UUID;

/**
 * Kết quả một lượt thu hồi phân công quá hạn.
 *
 * <p>Hai danh sách tách bạch thay vì một {@code List<UUID>} mang hai nghĩa tuỳ nhánh:
 * trước đây cùng một endpoint trả id các phân công <em>đã đóng</em> khi chỉ thu hồi, và
 * id các phân công <em>mới tạo</em> khi có giao lại — client không có cách nào phân biệt.
 *
 * @param reclaimedAssignmentIds phân công cũ vừa bị đóng (luôn có khi thu hồi được gì đó)
 * @param reassignedAssignmentIds phân công mới vừa mở cho nhóm thay thế; rỗng khi admin
 *        chỉ thu hồi mà không chọn ai
 */
public record ReclaimOverdueResponse(
    List<UUID> reclaimedAssignmentIds,
    List<UUID> reassignedAssignmentIds
) {
    public static ReclaimOverdueResponse empty() {
        return new ReclaimOverdueResponse(List.of(), List.of());
    }
}
