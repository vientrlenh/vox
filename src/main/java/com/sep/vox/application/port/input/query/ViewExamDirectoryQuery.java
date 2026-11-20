package com.sep.vox.application.port.input.query;

import java.util.List;
import java.util.UUID;

/**
 * Input chung của bốn query danh bạ kỳ thi (lớp / niên khóa / học sinh / giám thị) —
 * cả bốn nhận đúng bộ tham số này nên dùng chung một record thay vì bốn bản sao.
 *
 * <p>`page` là 1-based.
 *
 * <p>{@code excludeUserIds}: những người GỌI ĐÃ CÓ rồi (thí sinh của kỳ thi, giám thị của ca) và
 * không muốn thấy nữa. Phải lọc ngay trong SQL chứ không để client bỏ đi sau khi nhận trang: lọc ở
 * client thì `content` ngắn đi trong khi `totalElements`/`totalPages` vẫn đếm cả người bị bỏ — nhập
 * xong một lớp là picker hiện trang trống kèm số đếm khác 0. Hai query lớp/niên khóa bỏ qua trường
 * này.
 */
public record ViewExamDirectoryQuery(
    UUID examId,
    String search,
    int page,
    int size,
    List<UUID> excludeUserIds
) {
    public ViewExamDirectoryQuery {
        excludeUserIds = excludeUserIds == null ? List.of() : List.copyOf(excludeUserIds);
    }
}
