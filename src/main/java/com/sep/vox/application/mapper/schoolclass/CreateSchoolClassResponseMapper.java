package com.sep.vox.application.mapper.schoolclass;

import java.util.UUID;

import com.sep.vox.application.response.input.schoolclass.CreateSchoolClassResponse;

public final class CreateSchoolClassResponseMapper {
    
    public static CreateSchoolClassResponse toResponse(UUID schoolClassId) {
        return new CreateSchoolClassResponse(schoolClassId);
    }
}
