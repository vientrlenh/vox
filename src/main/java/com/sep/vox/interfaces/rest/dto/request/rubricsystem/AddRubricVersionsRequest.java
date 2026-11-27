package com.sep.vox.interfaces.rest.dto.request.rubricsystem;

import com.sep.vox.application.port.input.command.rubricschool.AddSchoolRubricVersionsCommand;
import com.sep.vox.application.port.input.command.rubricsystem.AddSystemRubricVersionsCommand;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AddRubricVersionsRequest(
        @NotEmpty(message = "Version list must not be blank")
        @Valid
        List<RubricVersionItemRequest> versions
) {
    public record RubricVersionItemRequest(
            @NotNull(message = "Version number must not be blank")
            @Positive(message = "Version number must be greater than 0")
            Integer version,

            @NotBlank(message = "Version name must not be blank")
            String name,

            @NotNull(message = "Floor score must not be blank")
            @DecimalMin(value = "0.0", message = "Floor score must be greater than or equal to 0")
            BigDecimal scoringScaleMin,

            @NotNull(message = "Ceiling score must not be blank")
            @DecimalMin(value = "0.0", inclusive = false, message = "Ceiling score must be greater than 0")
            BigDecimal scoringScaleMax,

            @NotNull(message = "Total score method must not be blank (SUM/WEIGHTED_AVERAGE)")
            RubricTotalScoreMethod totalScoreMethod,

            Instant effectiveFrom,

            Instant effectiveTo
    ) {}

    // FOR SYSTEM ADMIN
    public static AddSystemRubricVersionsCommand toAddSystemRubricVersionsCommand(UUID rubricId, AddRubricVersionsRequest request) {
        List<AddSystemRubricVersionsCommand.RubricVersionItemCommand> items = request.versions().stream()
                .map(v -> new AddSystemRubricVersionsCommand.RubricVersionItemCommand(
                        v.version(), v.name(), v.scoringScaleMin(), v.scoringScaleMax(), v.totalScoreMethod(),
                        v.effectiveFrom(), v.effectiveTo()
                )).toList();
        return new AddSystemRubricVersionsCommand(rubricId, items);
    }

    // FOR SCHOOL ADMIN
    public static AddSchoolRubricVersionsCommand toAddSchoolRubricVersionsCommand(UUID schoolId, UUID rubricId, AddRubricVersionsRequest request) {
        List<AddSchoolRubricVersionsCommand.RubricVersionItemCommand> items = request.versions().stream()
                .map(v -> new AddSchoolRubricVersionsCommand.RubricVersionItemCommand(
                        v.version(), v.name(), v.scoringScaleMin(), v.scoringScaleMax(), v.totalScoreMethod(),
                        v.effectiveFrom(), v.effectiveTo()
                )).toList();
        return new AddSchoolRubricVersionsCommand(schoolId, rubricId, items);
    }
}
