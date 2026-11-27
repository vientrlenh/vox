package com.sep.vox.interfaces.rest.dto.request.rubricschool;

import com.sep.vox.application.port.input.command.rubricschool.CreateSchoolRubricCriteriaCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateSchoolRubricCriteriaRequest(
        @NotEmpty(message = "At least 1 criterion must be submitted")
        @Valid
        List<CriterionItemRequest> criteria
) {
    public record CriterionItemRequest(
            @NotNull(message = "Framework Criterion ID must not be blank")
            UUID frameworkCriterionId,

            @NotBlank(message = "Criterion code must not be blank")
            String code,

            @NotBlank(message = "Criterion name must not be blank")
            String name,

            String description,

            @Valid // So that Spring also validates the examples inside
            List<CriterionExampleRequest> examples,

            @NotNull(message = "Weight must not be blank")
            @DecimalMin(value = "0.0", message = "Weight must be greater than or equal to 0")
            BigDecimal weight,

            @NotNull(message = "Minimum score must not be blank")
            BigDecimal minScore,

            @NotNull(message = "Maximum score must not be blank")
            BigDecimal maxScore,

            @NotNull(message = "Order must not be blank")
            @Min(value = 1, message = "Order must be greater than 0")
            Integer order,

            @NotNull(message = "Is Required flag must not be blank")
            Boolean isRequired
    ) {
        private CreateSchoolRubricCriteriaCommand.CriterionItemCommand asCriterionItemCommand() {
            return new CreateSchoolRubricCriteriaCommand.CriterionItemCommand(
                    frameworkCriterionId,
                    code,
                    name,
                    description,
                    asExampleCommands(),
                    weight,
                    minScore,
                    maxScore,
                    order,
                    isRequired
            );
        }

        private List<CreateSchoolRubricCriteriaCommand.CriterionExampleCommand> asExampleCommands() {
            if (examples == null) {
                return null;
            }
            return examples.stream()
                    .map(example -> new CreateSchoolRubricCriteriaCommand.CriterionExampleCommand(
                            example.transcript(), example.explanation(), example.expectedScore()
                    )).toList();
        }
    }

    public record CriterionExampleRequest(
            @NotBlank(message = "Example content (transcript) must not be blank")
            String transcript,

            @NotBlank(message = "Explanation must not be blank")
            String explanation,

            @NotNull(message = "Expected score must not be blank")
            BigDecimal expectedScore
    ) {}

    public static CreateSchoolRubricCriteriaCommand toCommand(UUID schoolId, UUID versionId, CreateSchoolRubricCriteriaRequest request) {

        List<CreateSchoolRubricCriteriaCommand.CriterionItemCommand> criteriaCommands = request.criteria().stream()
                .map(r -> r.asCriterionItemCommand())
                .toList();

        return new CreateSchoolRubricCriteriaCommand(schoolId, versionId, criteriaCommands);
    }
}
