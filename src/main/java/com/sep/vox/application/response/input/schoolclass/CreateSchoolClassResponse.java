package com.sep.vox.application.response.input.schoolclass;

import java.util.UUID;

public record CreateSchoolClassResponse(
    UUID schoolClassId
) {

    public static CreateSchoolClassResponse toResponse(UUID schoolClassId) {
        return new CreateSchoolClassResponse(schoolClassId);
    }
}
