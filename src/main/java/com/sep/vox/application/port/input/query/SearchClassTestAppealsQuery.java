package com.sep.vox.application.port.input.query;

import java.util.UUID;

/** Đơn phúc khảo của MỘT bài kiểm tra trên lớp — màn của giáo viên tạo bài. */
public record SearchClassTestAppealsQuery(
    UUID examId,
    String status,
    String keyword,
    int page,
    int size
) {
}
