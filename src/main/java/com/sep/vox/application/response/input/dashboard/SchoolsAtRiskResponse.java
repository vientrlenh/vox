package com.sep.vox.application.response.input.dashboard;

import com.sep.vox.application.query.dto.SchoolAtRiskDto;
import com.sep.vox.application.query.dto.SchoolRiskBucket;
import com.sep.vox.domain.common.PageResult;

/**
 * Trang "trường cần chú ý": số đếm của cả bốn nhóm, cộng danh sách của nhóm đang mở.
 *
 * <p>Bốn số đếm đi kèm trong CÙNG một phản hồi, và lấy từ CHÍNH phép đếm mà trang tổng quan dùng —
 * không đếm lại bằng câu khác. Đây là điểm dễ hỏng nhất của màn hình này: thẻ trên trang tổng quan
 * ghi 5, người dùng bấm vào, và nếu con số ở đây được dựng bằng một phép lọc khác thì họ thấy 9.
 *
 * @param counts số đếm theo nhóm, cùng thứ tự ý nghĩa với {@link SchoolRiskBucket}; ĐỪNG cộng lại
 */
public record SchoolsAtRiskResponse(
    SchoolRiskBucket bucket,
    BucketCounts counts,
    PageResult<SchoolAtRiskDto> schools
) {

    public record BucketCounts(
        long expiringSoon,
        long lapsed,
        long suspended,
        long inDebt
    ) {
    }
}
