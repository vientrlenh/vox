package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.CreateSchoolRubricApplicabilityCommand;
import com.sep.vox.application.port.input.command.CreateSystemRubricApplicabilityCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolRubricApplicabilityRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemRubricApplicabilityRequest;

import java.util.UUID;
import java.util.stream.Collectors;

public class CreateRubricApplicabilityCommandMapper {
    public static CreateSchoolRubricApplicabilityCommand fromSchoolRequest(UUID schoolId, UUID versionId, CreateSchoolRubricApplicabilityRequest request) {
        var items = request.applicabilities().stream()
                .map(a -> new CreateSchoolRubricApplicabilityCommand.ApplicabilityItemCommand(
                        a.schoolGradeId(),
                        a.schoolClassId(),
                        DateMapper.toOffsetDateTime(a.effectiveFrom()),
                        DateMapper.toOffsetDateTime(a.effectiveTo())
                )).collect(Collectors.toList());

        return new CreateSchoolRubricApplicabilityCommand(schoolId, versionId, items);

    }

    public static CreateSystemRubricApplicabilityCommand fromSystemRequest(UUID versionId, CreateSystemRubricApplicabilityRequest request) {
        var items = request.applicabilities().stream()
                .map(a -> new CreateSystemRubricApplicabilityCommand.ApplicabilityItemCommand(
                        DateMapper.toOffsetDateTime(a.effectiveFrom()),
                        DateMapper.toOffsetDateTime(a.effectiveTo())
                )).collect(Collectors.toList());

        return new CreateSystemRubricApplicabilityCommand(versionId, items);
    }

}