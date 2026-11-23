package com.sep.vox.interfaces.rest.dto.request.rubricschool;

import com.sep.vox.application.port.input.command.rubricschool.CloneSystemRubricToSchoolCommand;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CloneSystemRubricRequest(
        @NotNull(message = "Phiên bản bộ tiêu chí mẫu không được để trống")
        UUID sourceRubricVersionId,

        // The code is set by the SCHOOL, not copied from the template: it tells apart the copies of the same
        // template (ENG-K10 / ENG-K11 / ENG-K12) and it is also what the unique constraint of the rubrics table checks.
        @NotBlank(message = "Mã bộ tiêu chí không được để trống")
        @Size(max = 50, message = "Mã bộ tiêu chí tối đa 50 ký tự")
        String code,

        @NotBlank(message = "Tên bộ tiêu chí không được để trống")
        @Size(max = 255, message = "Tên bộ tiêu chí tối đa 255 ký tự")
        String name,

        @Size(max = 2048, message = "Mô tả tối đa 2048 ký tự")
        String description,

        /**
         * Scoring method the school picks for the copy; empty keeps the same as the template.
         *
         * <p>Choosing WEIGHTED_AVERAGE means dropping the template's allocation ratios and weighting every criterion
         * equally -- that is what that method means, not an unintended loss. The opposite direction
         * cannot be derived, so the template should be authored as SUM.
         */
        RubricTotalScoreMethod totalScoreMethod,

        /**
         * Sample scoring policies (attached to the very version being copied) that the school wants to create for
         * the copy. Empty = copy only the criteria set; the new version then stays DRAFT until the school attaches
         * a policy itself, because publishing a version requires a linked policy that is already PUBLISHED.
         */
        @Valid
        List<ClonePolicyChoice> policies
) {

    /**
     * The scope follows EACH policy, not the whole copy: each scope may have exactly one
     * active policy, so copying two templates (for example Level 3 for regular classes, Level 4 for
     * specialised classes) into the same scope is rejected within the same call.
     */
    public record ClonePolicyChoice(
            @NotNull(message = "Chính sách mẫu không được để trống")
            UUID sourcePolicyId,

            // Only filled when the template is NOT attached to a Grade; then exactly 1 of the 3 must be chosen. A template
            // attached to a Grade keeps that grade in the copy and all three must be left empty.
            UUID gradeLevelId,
            UUID schoolGradeId,
            UUID schoolClassId,

            @NotNull(message = "Ngày bắt đầu hiệu lực không được để trống")
            Instant effectiveFrom,

            Instant effectiveTo
    ) {}

    public static CloneSystemRubricToSchoolCommand toCommand(UUID schoolId, CloneSystemRubricRequest request) {
        return new CloneSystemRubricToSchoolCommand(
                schoolId,
                request.sourceRubricVersionId(),
                request.code(),
                request.name(),
                request.description(),
                request.totalScoreMethod(),
                toPolicies(request.policies())
        );
    }

    /** An absent list and an empty list both mean "copy only the criteria set", so they are normalised to one form right here. */
    private static List<CloneSystemRubricToSchoolCommand.PolicyToClone> toPolicies(
            List<CloneSystemRubricRequest.ClonePolicyChoice> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return raw.stream()
                .map(choice -> new CloneSystemRubricToSchoolCommand.PolicyToClone(
                        choice.sourcePolicyId(),
                        choice.gradeLevelId(),
                        choice.schoolGradeId(),
                        choice.schoolClassId(),
                        choice.effectiveFrom(),
                        choice.effectiveTo()))
                .toList();
    }
}
