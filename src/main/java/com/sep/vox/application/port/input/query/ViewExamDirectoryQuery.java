package com.sep.vox.application.port.input.query;

import java.util.UUID;

/**
 * Input chung của bốn query danh bạ kỳ thi (lớp / niên khóa / học sinh / giám thị) —
 * cả bốn nhận đúng bộ tham số này nên dùng chung một record thay vì bốn bản sao.
 *
 * <p>`page` là 1-based.
 */
public record ViewExamDirectoryQuery(
    UUID examId,
    String search,
    int page,
    int size
) {
}
