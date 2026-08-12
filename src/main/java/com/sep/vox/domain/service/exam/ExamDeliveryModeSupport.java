package com.sep.vox.domain.service.exam;

import com.sep.vox.domain.model.exam.ExamDeliveryMode;

/**
 * Parse hình thức làm bài từ chuỗi người dùng gửi lên.
 *
 * <p>Gom về một chỗ vì cả lúc tạo bài (kỳ thi tập trung, bài kiểm tra trên lớp) lẫn lúc đổi hình
 * thức đều cần đúng một thông báo lỗi tiếng Việt.
 */
public final class ExamDeliveryModeSupport {

    private ExamDeliveryModeSupport() {
    }

    /** Parse giá trị bắt buộc; ném IllegalArgumentException nếu không hợp lệ. */
    public static ExamDeliveryMode parse(String value) {
        try {
            return ExamDeliveryMode.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Hình thức làm bài không hợp lệ");
        }
    }

    /** Như trên nhưng cho phép bỏ trống — trả về {@code fallback} khi người dùng không chọn. */
    public static ExamDeliveryMode parseOrDefault(String value, ExamDeliveryMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return parse(value);
    }
}
