package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolRubricResultBandDetailsQuery(
        UUID schoolId,
        UUID resultBandId
) {}