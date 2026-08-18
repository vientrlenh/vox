package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.CreateAssessmentPolicyCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolAssessmentPolicyRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemAssessmentPolicyRequest;

import java.util.List;
import java.util.UUID;

public class CreateAssessmentPolicyCommandMapper {

    // DÀNH CHO SYSTEM ADMIN: schoolId và scope (grade/class) luôn null -> policy áp dụng toàn hệ thống
    public static CreateAssessmentPolicyCommand fromSystemRequest(CreateSystemAssessmentPolicyRequest request) {
        return new CreateAssessmentPolicyCommand(
                null,
                request.frameworkVersionId(),
                request.rubricVersionId(),
                request.languageId(),
                null,
                null,
                null,
                request.targetFrameworkBandId(),
                request.passingScore(),
                request.strictness(),
                DateMapper.toInstant(request.effectiveFrom()),
                DateMapper.toInstant(request.effectiveTo())
        );
    }

    // DÀNH CHO SYSTEM ADMIN: cho phép gửi 1 lúc nhiều Assessment Policy trong 1 request
    public static List<CreateAssessmentPolicyCommand> fromSystemRequests(List<CreateSystemAssessmentPolicyRequest> requests) {
        return requests.stream().map(CreateAssessmentPolicyCommandMapper::fromSystemRequest).toList();
    }

    // DÀNH CHO SCHOOL ADMIN
    public static CreateAssessmentPolicyCommand fromSchoolRequest(UUID schoolId, CreateSchoolAssessmentPolicyRequest request) {
        return new CreateAssessmentPolicyCommand(
                schoolId,
                request.frameworkVersionId(),
                request.rubricVersionId(),
                request.languageId(),
                request.schoolGradeLevelId(),
                request.schoolGradeId(),
                request.schoolClassId(),
                request.targetFrameworkBandId(),
                request.passingScore(),
                request.strictness(),
                DateMapper.toInstant(request.effectiveFrom()),
                DateMapper.toInstant(request.effectiveTo())
        );
    }

    // DÀNH CHO SCHOOL ADMIN: cho phép gửi 1 lúc nhiều Assessment Policy trong 1 request
    public static List<CreateAssessmentPolicyCommand> fromSchoolRequests(UUID schoolId, List<CreateSchoolAssessmentPolicyRequest> requests) {
        return requests.stream().map(request -> fromSchoolRequest(schoolId, request)).toList();
    }
}
