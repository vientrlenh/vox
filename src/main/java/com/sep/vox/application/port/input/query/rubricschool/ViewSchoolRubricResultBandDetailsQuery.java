package com.sep.vox.application.port.input.query.rubricschool;

import java.util.UUID;

public record ViewSchoolRubricResultBandDetailsQuery(
        UUID schoolId,
        UUID resultBandId
) {}