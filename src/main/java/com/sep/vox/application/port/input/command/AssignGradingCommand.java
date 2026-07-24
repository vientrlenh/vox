package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

/**
 * Gán tay nhiều bài trong một lần. Batch là cố ý: admin tick nhiều dòng trên bảng
 * rồi gán một phát, và ràng buộc "một giáo viên / bài" được kiểm trọn trong một
 * transaction thay vì rải rác qua N request.
 */
public record AssignGradingCommand(
    List<AssignmentItem> assignments
) {
    public record AssignmentItem(
        UUID candidateResultId,
        UUID teacherId
    ) {
    }
}
