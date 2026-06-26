package com.sep.vox.interfaces.graphql.dto.request;

import java.time.LocalDate;

public record UpdateSchoolGradeRequest (
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate
){}
