package com.sep.vox.interfaces.rest.mapper;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.CloneSystemRubricToSchoolCommand;
import com.sep.vox.interfaces.rest.dto.request.CloneSystemRubricRequest;

public class CloneSystemRubricCommandMapper {

    public static CloneSystemRubricToSchoolCommand fromRequest(UUID schoolId, CloneSystemRubricRequest request) {
        return new CloneSystemRubricToSchoolCommand(
                schoolId,
                request.sourceRubricVersionId(),
                request.code(),
                request.name(),
                request.description(),
                request.totalScoreMethod(),
                toPolicies(request.policies())
        );
    }

    /** Vắng mặt và danh sách rỗng cùng nghĩa "chỉ sao bộ tiêu chí", nên quy về một dạng ngay ở đây. */
    private static List<CloneSystemRubricToSchoolCommand.PolicyToClone> toPolicies(
            List<CloneSystemRubricRequest.ClonePolicyChoice> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return raw.stream()
                .map(choice -> new CloneSystemRubricToSchoolCommand.PolicyToClone(
                        choice.sourcePolicyId(),
                        choice.gradeLevelId(),
                        choice.schoolGradeId(),
                        choice.schoolClassId(),
                        choice.effectiveFrom(),
                        choice.effectiveTo()))
                .toList();
    }
}
