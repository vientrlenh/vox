package com.sep.vox.interfaces.graphql.dto.request;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateSchoolGradeRequest (
        UUID schoolGradeId,
        UUID schoolId,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate
){}
