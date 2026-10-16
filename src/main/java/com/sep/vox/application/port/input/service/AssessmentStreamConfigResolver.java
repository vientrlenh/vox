package com.sep.vox.application.port.input.service;

import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;

/**
 * Chuyển cấu hình stream thô của người tạo bài ({@code ["CAMERA","SCREEN"]} + quyền) thành cặp
 * {@code requiredStreamType}/{@code streamTypePermission} lưu trên Assessment.
 *
 * <p>Dùng chung cho cả kỳ thi tập trung lẫn bài kiểm tra trên lớp: hai luồng tạo phải ra cùng một
 * kết quả, nếu để mỗi bên tự validate thì rất dễ lệch và sinh ra assessment có stream config nửa vời mà
 * IssueStudentStreamTokenUseCase không phát token được.
 */
@Service
public class AssessmentStreamConfigResolver {

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
            return new StreamConfig(AssessmentRequiredStreamType.valueOf(types.iterator().next()), null);
        }

        if (rawPermission == null) {
            throw new IllegalArgumentException("Quyền cho các stream là bắt buộc cho 2 loại đồng thời");
        }
        if (!PERMITTED_STREAM_PERMISSION.contains(rawPermission)) {
            throw new IllegalArgumentException("Quyền cho các stream không hợp lệ");
        }
        return new StreamConfig(
            AssessmentRequiredStreamType.CAMERA_AND_SCREEN,
            AssessmentStreamTypePermission.valueOf(rawPermission)
        );
    }

    public record StreamConfig(
        AssessmentRequiredStreamType requiredStreamType,
        AssessmentStreamTypePermission streamTypePermission
    ) {
    }
}
