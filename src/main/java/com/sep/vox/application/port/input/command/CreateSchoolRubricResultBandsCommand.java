package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateSchoolRubricResultBandsCommand(
        UUID schoolId,
        UUID versionId,
        List<ResultBandItemCommand> resultBands
) {
    public record ResultBandItemCommand(
            String code,
            String name,
            String description,
            BigDecimal mappedScoreMin,
            BigDecimal mappedScoreMax,
            Integer order
    ) {}
}