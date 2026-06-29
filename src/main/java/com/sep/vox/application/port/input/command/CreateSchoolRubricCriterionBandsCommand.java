package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateSchoolRubricCriterionBandsCommand(
        UUID schoolId,
        UUID versionId,
        UUID criterionId,
        List<CriterionBandItemCommand> bands
) {
    public record CriterionBandItemCommand(
            String code,
            BigDecimal scoreMin,
            BigDecimal scoreMax
    ) {}
}