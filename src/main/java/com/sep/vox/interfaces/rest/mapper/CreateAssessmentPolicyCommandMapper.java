package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.CreateAssessmentPolicyCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolAssessmentPolicyRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemAssessmentPolicyRequest;

import java.util.UUID;

public class CreateAssessmentPolicyCommandMapper {

    // DÀNH CHO SYSTEM ADMIN: schoolId và scope (grade/class) luôn null -> policy áp dụng toàn hệ thống
    public static CreateAssessmentPolicyCommand fromSystemRequest(CreateSystemAssessmentPolicyRequest request) {
        return new CreateAssessmentPolicyCommand(
                null,
                request.frameworkVersionId(),
                request.rubricVersionIds(),
                request.languageId(),
                null,
                null,
                null,
                request.targetFrameworkBandId(),
                request.minimumFrameworkBandId(),
                request.passingScore(),
                request.strictness(),
                DateMapper.toOffsetDateTime(request.effectiveFrom()),
                DateMapper.toOffsetDateTime(request.effectiveTo())
        );
    }

    // DÀNH CHO SCHOOL ADMIN
    public static CreateAssessmentPolicyCommand fromSchoolRequest(UUID schoolId, CreateSchoolAssessmentPolicyRequest request) {
        return new CreateAssessmentPolicyCommand(
                schoolId,
                request.frameworkVersionId(),
                request.rubricVersionIds(),
                request.languageId(),
                request.schoolGradeLevelId(),
                request.schoolGradeId(),
                request.schoolClassId(),
                request.targetFrameworkBandId(),
                request.minimumFrameworkBandId(),
                request.passingScore(),
                request.strictness(),
                DateMapper.toOffsetDateTime(request.effectiveFrom()),
                DateMapper.toOffsetDateTime(request.effectiveTo())
        );
    }
}
