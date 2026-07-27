package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;


public record IssueStudentStreamTokenRequest(
    @NotNull(message = "Phiên thi không được để trống")
    UUID examSessionId,

    /**
     * Loại stream học viên muốn dùng: CAMERA, SCREEN hoặc CAMERA_AND_SCREEN (không phân biệt hoa
     * thường).
     *
     * <p>Để trống là hợp lệ và là trường hợp thường gặp nhất: server đã biết kỳ thi yêu cầu gì
     * ({@code exam.requiredStreamType}) nên không cần client nói lại. Chỉ khi kỳ thi đặt
     * {@code streamTypePermission = ANY} thì trường này mới thực sự có tác dụng, vì đó là cấu hình
     * duy nhất cho học viên nhiều hơn một lựa chọn hợp lệ.
     *
     * <p>Lựa chọn được chốt ở lần phát token đầu tiên của phiên thi; các lần sau chỉ được gửi lại
     * đúng giá trị đó hoặc để trống.
     */
    String streamType
) {

}
