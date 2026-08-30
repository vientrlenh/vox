package com.sep.vox.application.port.input.query;

import java.util.UUID;

/**
 * @param search lọc theo TÊN người dùng, null/rỗng = không lọc. Lọc ở máy chủ chứ không ở giao diện:
 *               từ khi danh sách có phân trang, lọc trên một trang 20 dòng sẽ bỏ sót đúng người mà
 *               quản trị viên đang tìm.
 * @param page   đếm TỪ 1 theo quy ước chung của dự án
 */
public record ViewQuotaUserAllocationsQuery(
    UUID schoolId,
    String search,
    int page,
    int size
) {
}
