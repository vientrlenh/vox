package com.sep.vox.application.port.input.query;

import com.sep.vox.application.projection.dto.SchoolRiskBucket;
import com.sep.vox.application.shared.AppPageRequest;

/**
 * @param bucket  nhóm đang mở; bắt buộc — trang này luôn đứng trong đúng một nhóm
 * @param keyword tìm theo tên hoặc mã trường; bỏ trống = không lọc
 */
public record ViewSchoolsAtRiskQuery(
    SchoolRiskBucket bucket,
    String keyword,
    AppPageRequest pageRequest
) {
}
