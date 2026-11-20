package com.sep.vox.application.port.input.service;

import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.exam.ExamRequiredStreamType;
import com.sep.vox.domain.model.exam.ExamStreamTypePermission;

/**
 * Chuyển cấu hình stream thô của người tạo bài ({@code ["CAMERA","SCREEN"]} + quyền) thành cặp
 * {@code requiredStreamType}/{@code streamTypePermission} lưu trên Exam.
 *
 * <p>Dùng chung cho cả kỳ thi tập trung lẫn bài kiểm tra trên lớp: hai luồng tạo phải ra cùng một
 * kết quả, nếu để mỗi bên tự validate thì rất dễ lệch và sinh ra exam có stream config nửa vời mà
 * IssueStudentStreamTokenUseCase không phát token được.
 */
@Service
public class ExamStreamConfigResolver {

    private static final List<String> PERMITTED_STREAM_TYPES = List.of("CAMERA", "SCREEN");
    private static final List<String> PERMITTED_STREAM_PERMISSION = List.of("ALL", "ANY");

    public StreamConfig resolve(List<String> rawTypes, String rawPermission) {
        if (rawTypes == null || rawTypes.isEmpty()) {
            if (rawPermission != null) {
                throw new IllegalArgumentException("Không thể đặt quyền stream khi không yêu cầu stream nào");
            }
            return new StreamConfig(null, null);
        }

        var types = new LinkedHashSet<>(rawTypes);
        if (types.contains(null) || !PERMITTED_STREAM_TYPES.containsAll(types)) {
            throw new IllegalArgumentException("Các loại stream gửi yêu cầu không được hỗ trợ");
        }

        if (types.size() == 1) {
            if (rawPermission != null) {
                throw new IllegalArgumentException("Quyền stream chỉ áp dụng khi yêu cầu đồng thời cả hai loại stream");
            }
            return new StreamConfig(ExamRequiredStreamType.valueOf(types.iterator().next()), null);
        }

        if (rawPermission == null) {
            throw new IllegalArgumentException("Quyền cho các stream là bắt buộc cho 2 loại đồng thời");
        }
        if (!PERMITTED_STREAM_PERMISSION.contains(rawPermission)) {
            throw new IllegalArgumentException("Quyền cho các stream không hợp lệ");
        }
        return new StreamConfig(
            ExamRequiredStreamType.CAMERA_AND_SCREEN,
            ExamStreamTypePermission.valueOf(rawPermission)
        );
    }

    public record StreamConfig(
        ExamRequiredStreamType requiredStreamType,
        ExamStreamTypePermission streamTypePermission
    ) {
    }
}
