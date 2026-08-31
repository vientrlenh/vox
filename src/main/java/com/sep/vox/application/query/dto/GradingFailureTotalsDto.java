package com.sep.vox.application.query.dto;

/**
 * Dải tóm tắt đầu trang phân loại phiên chấm lỗi.
 *
 * <p>Tính bằng một câu riêng chứ không cộng lại danh sách nhóm: danh sách nhóm bị giới hạn số dòng,
 * và {@code schoolCount} là số trường KHÁC NHAU trên toàn bộ tập lỗi — cộng số trường của từng nhóm
 * sẽ đếm trùng đúng những trường dính nhiều nguyên nhân.
 *
 * @param sessionCount   phải khớp đúng ô "Phiên AI chấm lỗi" của trang tổng quan; cùng vị từ, cùng
 *                       cửa sổ theo {@code submitted_at}
 * @param causeCount     số nhóm nguyên nhân, tính cả nhóm "không rõ nguyên nhân" nếu có
 * @param schoolCount    số trường khác nhau bị ảnh hưởng
 * @param retryableCount số phiên chấm lại được — con số nút hàng loạt phải hiển thị, để người dùng
 *                       biết trước phần bị chặn thay vì bấm xong mới nhận một loạt lỗi
 */
public record GradingFailureTotalsDto(
    long sessionCount,
    long causeCount,
    long schoolCount,
    long retryableCount
) {
}
