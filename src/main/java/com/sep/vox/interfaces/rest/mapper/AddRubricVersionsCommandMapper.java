package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.AddSchoolRubricVersionsCommand;
import com.sep.vox.application.port.input.command.AddSystemRubricVersionsCommand;
import com.sep.vox.interfaces.rest.dto.request.AddRubricVersionsRequest; // Có thể đổi tên DTO này thành AddRubricVersionsRequest

import java.util.UUID;

public class AddRubricVersionsCommandMapper {

    //DÀNH CHO SYSTEM ADMIN
    public static AddSystemRubricVersionsCommand fromSystemRequest(UUID rubricId, AddRubricVersionsRequest request) {
        var items = request.versions().stream()
                .map(v -> new AddSystemRubricVersionsCommand.RubricVersionItemCommand(
                        v.version(), v.name(), v.scoringScaleMin(), v.scoringScaleMax(), v.totalScoreMethod(),
                        DateMapper.toInstant(v.effectiveFrom()), DateMapper.toInstant(v.effectiveTo())
                )).toList();
        return new AddSystemRubricVersionsCommand(rubricId, items);
    }

    //DÀNH CHO SCHOOL ADMIN
    public static AddSchoolRubricVersionsCommand fromSchoolRequest(UUID schoolId, UUID rubricId, AddRubricVersionsRequest request) {
        var items = request.versions().stream()
                .map(v -> new AddSchoolRubricVersionsCommand.RubricVersionItemCommand(
                        v.version(), v.name(), v.scoringScaleMin(), v.scoringScaleMax(), v.totalScoreMethod(),
                        DateMapper.toInstant(v.effectiveFrom()), DateMapper.toInstant(v.effectiveTo())
                )).toList();
        return new AddSchoolRubricVersionsCommand(schoolId, rubricId, items);
    }
}
