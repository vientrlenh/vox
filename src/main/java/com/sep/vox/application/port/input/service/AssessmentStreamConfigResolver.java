package com.sep.vox.application.port.input.service;

import java.util.LinkedHashSet;
import java.util.List;

import java.util.Set;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;

/**
 * Converts the raw stream configuration of the assessment creator ({@code ["CAMERA","SCREEN"]} + permission) into the pair
 * {@code requiredStreamType}/{@code streamTypePermission} stored on the Assessment.
 *
 * <p>Shared by centralized assessments and class tests: the two creation flows must produce the same
 * result. If each side validated by itself they could easily diverge and create an assessment with a half-baked stream config for which
 * IssueStudentStreamTokenUseCase cannot issue a token.
 */
@Service
public class AssessmentStreamConfigResolver {

    private static final List<String> PERMITTED_STREAM_TYPES = List.of("CAMERA", "SCREEN");

    public StreamConfig resolve(List<String> rawTypes, AssessmentStreamTypePermission rawPermission) {
        if (rawTypes == null || rawTypes.isEmpty()) {
            if (rawPermission != null) {
                throw new IllegalArgumentException("Không thể đặt quyền stream khi không yêu cầu stream nào");
            }
            return new StreamConfig(null, null);
        }

        Set<String> types = new LinkedHashSet<>(rawTypes);
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
        return new StreamConfig(
            AssessmentRequiredStreamType.CAMERA_AND_SCREEN,
            rawPermission
        );
    }

    public record StreamConfig(
        AssessmentRequiredStreamType requiredStreamType,
        AssessmentStreamTypePermission streamTypePermission
    ) {
    }
}
