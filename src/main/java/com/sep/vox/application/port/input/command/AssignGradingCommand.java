package com.sep.vox.application.port.input.command;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Gán tay nhiều bài trong một lần. Batch là cố ý: admin tick nhiều dòng trên bảng
 * rồi gán một phát, và ràng buộc "một phân công mở / bài" được kiểm trọn trong một
 * transaction thay vì rải rác qua N request.
 *
 * @param roundType  vòng chấm áp cho cả lô. Một lần gán chỉ một vòng — trộn vòng
 *                   trong cùng lô làm luật kiểm tra trạng thái bài nhập nhằng, mà
 *                   trên UI admin cũng chọn vòng trước rồi mới tick bài.
 * @param deadlineAt hạn chấm chung, có thể null
 */
public record AssignGradingCommand(
    String roundType,
    Instant deadlineAt,
    List<AssignmentItem> assignments
) {
    public record AssignmentItem(
        UUID candidateResultId,
        UUID teacherId
    ) {
    }
}
