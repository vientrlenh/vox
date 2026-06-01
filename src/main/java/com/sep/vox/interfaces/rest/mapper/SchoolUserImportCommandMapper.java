package com.sep.vox.interfaces.rest.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.sep.vox.application.port.input.command.ImportFieldMapping;
import com.sep.vox.application.port.input.command.ImportSchoolUsersCommand;
import com.sep.vox.interfaces.rest.dto.request.ImportFieldMappingRequest;
import com.sep.vox.interfaces.rest.dto.request.SchoolUserImportRequest;

public final class SchoolUserImportCommandMapper {

    private SchoolUserImportCommandMapper() {
    }

    public static ImportSchoolUsersCommand fromRequest(UUID schoolId, SchoolUserImportRequest request) {
        return new ImportSchoolUsersCommand(
            schoolId,
            request.fileId(),
            request.dryRun(),
            request.defaultRole(),
            mapMapping(request.mapping())
        );
    }

    private static Map<String, ImportFieldMapping> mapMapping(Map<String, ImportFieldMappingRequest> mapping) {
        if (mapping == null) {
            return Map.of();
        }
        var result = new HashMap<String, ImportFieldMapping>();
        for (var entry : mapping.entrySet()) {
            var value = entry.getValue();
            if (value == null) {
                continue;
            }
            result.put(entry.getKey(), new ImportFieldMapping(
                value.column(),
                value.index(),
                value.aliases(),
                value.path(),
                value.dateFormat()
            ));
        }
        return result;
    }
}
