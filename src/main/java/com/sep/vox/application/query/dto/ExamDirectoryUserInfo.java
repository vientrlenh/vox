package com.sep.vox.application.query.dto;

import java.util.UUID;

/**
 * Học sinh / giáo viên hiển thị trong danh bạ kỳ thi.
 *
 * <p>Cố tình phẳng (fullName, email nằm thẳng ở đây thay vì lồng type `User`): bề mặt
 * danh bạ phải là nhánh lá trong graph, không mở thêm cạnh nào cho vai trò TEACHER —
 * cùng lý do đã ghi ở `myclass.graphqls`.
 */
public record ExamDirectoryUserInfo(
    UUID userId,
    String fullName,
    String email,
    String status
) {
}
