package com.sep.vox.interfaces.rest.mapper;

import java.util.Locale;
import java.util.UUID;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.CloneSystemAssessmentPolicyToSchoolCommand;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.interfaces.rest.dto.request.CloneSystemAssessmentPolicyRequest;

public class CloneSystemAssessmentPolicyCommandMapper {

    public static CloneSystemAssessmentPolicyToSchoolCommand fromRequest(
            UUID schoolId, CloneSystemAssessmentPolicyRequest request) {
        return new CloneSystemAssessmentPolicyToSchoolCommand(
                schoolId,
                request.sourcePolicyId(),
                request.rubricCode(),
                request.rubricName(),
                request.rubricDescription(),
                toTotalScoreMethod(request.totalScoreMethod()),
                request.gradeLevelId(),
                request.schoolGradeId(),
                request.schoolClassId(),
                DateMapper.toInstant(request.effectiveFrom()),
                DateMapper.toInstant(request.effectiveTo()));
    }

    /**
     * null khi client không chọn -- use case hiểu là "giữ nguyên như bản mẫu". Giá trị lạ đã bị
     * {@code @Pattern} trên request chặn từ trước.
     */
    private static RubricTotalScoreMethod toTotalScoreMethod(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return RubricTotalScoreMethod.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
