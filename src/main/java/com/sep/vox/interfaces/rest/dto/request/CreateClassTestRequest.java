package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ResultDecisionMethod;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateClassTestRequest(
    @NotNull(message = "Lớp học là bắt buộc")
    UUID schoolClassId,

    @NotBlank(message = "Tên bài kiểm tra là bắt buộc")
    String name,

    String description,

    @NotBlank(message = "Thời gian mở bài là bắt buộc")
    String openAt,

    @NotBlank(message = "Thời gian đóng bài là bắt buộc")
    String closeAt,

    @NotNull(message = "Bộ tiêu chí đánh giá là bắt buộc")
    UUID assessmentPolicyId,

    @Min(value = 1, message = "Số lượt thi tối đa phải lớn hơn 0")
    Integer maxAttempt,

    @Min(value = 1, message = "Thời lượng bài thi phải lớn hơn 0 giây")
    Integer examTimeDurationSecond,

    ResultDecisionMethod resultDecisionMethod,

    /** Các loại stream giám sát: CAMERA và/hoặc SCREEN. Bỏ trống = không giám sát bằng stream. */
    List<String> requiredStreamTypes,

    /** Quyền chọn stream, chỉ áp dụng khi yêu cầu cả hai loại: ALL (bắt buộc cả hai) hoặc ANY. */
    String streamTypePermission,

    /** Thiết bị làm bài: LAB = thiết bị nhà trường, STUDENT_DEVICE = thiết bị học sinh. Bỏ trống = STUDENT_DEVICE. */
    @Pattern(regexp = "STUDENT_DEVICE|LAB", message = "Hình thức làm bài không hợp lệ")
    String deliveryMode,

    /** Bắt học sinh nhập OTP của ca thi khi vào bài. Bỏ trống = không yêu cầu. */
    Boolean requiresOtp,

    /** Phòng thi. Có thể bỏ trống lúc tạo và chọn sau, nhưng phải có trước khi lên lịch. */
    UUID schoolRoomId
) {
}
