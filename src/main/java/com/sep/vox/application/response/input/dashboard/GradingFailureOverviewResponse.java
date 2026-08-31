package com.sep.vox.application.response.input.dashboard;

import java.util.List;

import com.sep.vox.application.query.dto.GradingFailureGroupDto;

/**
 * Trang phân loại phiên chấm lỗi: dải tóm tắt cộng danh sách nhóm nguyên nhân.
 *
 * @param groupsTruncated số nhóm đã vượt trần và bị cắt. Nói ra thay vì im lặng cắt, vì nó là DẤU
 *                        HIỆU CHẨN ĐOÁN: chuẩn hóa thông điệp lỗi mà không ăn thì số nhóm nở gần
 *                        bằng số phiên, và lúc đó việc gom nhóm không còn giúp gì — cần sửa hàm
 *                        {@code vox_grading_error_signature}, chứ không phải nới trần.
 */
public record GradingFailureOverviewResponse(
    long sessionCount,
    long causeCount,
    long schoolCount,
    long retryableCount,
    List<GradingFailureGroupDto> groups,
    long groupsTruncated
) {
}
