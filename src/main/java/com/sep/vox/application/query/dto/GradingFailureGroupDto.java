package com.sep.vox.application.query.dto;

import java.time.Instant;

/**
 * Một NHÓM phiên chấm lỗi cùng nguyên nhân.
 *
 * @param signature       chữ ký chuẩn hóa của thông điệp lỗi; {@code null} là nhóm "không rõ nguyên
 *                        nhân" — một nhóm thật, không phải dữ liệu thiếu (xem V6)
 * @param sampleError     một thông điệp thô đại diện của nhóm, để hiện nguyên văn cho người đọc;
 *                        {@code null} ở nhóm không rõ nguyên nhân
 * @param sessionCount    số phiên trong nhóm
 * @param schoolCount     số trường khác nhau; kỳ thi cấp hệ thống không thuộc trường nào nên không
 *                        được đếm
 * @param examCount       số kỳ thi khác nhau
 * @param firstFailedAt   mốc nộp sớm nhất trong nhóm — cùng với mốc muộn nhất, đây là thứ cho thấy
 *                        một sự cố gói gọn trong hai tiếng khác với một lỗi rỉ rả suốt hai tuần
 * @param lastFailedAt    mốc nộp muộn nhất trong nhóm
 * @param retryableCount  số phiên thật sự chấm lại được — xem {@link GradingFailureTotalsDto}
 */
public record GradingFailureGroupDto(
    String signature,
    String sampleError,
    long sessionCount,
    long schoolCount,
    long examCount,
    Instant firstFailedAt,
    Instant lastFailedAt,
    long retryableCount
) {
}
