package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.service.rubric.ScoreRangeValidator;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSchoolRubricVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateSchoolRubricVersionUseCase
        implements IUseCase<UpdateSchoolRubricVersionCommand, UUID> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricCriterionRepository rubricCriterionRepository;

    @Override
    @Transactional
    public UUID execute(UpdateSchoolRubricVersionCommand command) {
        // 1. Authenticate the account
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }

        // 2. Check that the user belongs to the right school
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() ->
                        new ForbiddenException("Tài khoản chưa được liên kết với trường học.")
                );

        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException(
                    "Không có quyền cập nhật dữ liệu của trường học khác."
            );
        }

        School school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy trường học.")
                );

        if (!school.isActive()) {
            throw new ForbiddenException("Trường học này đang bị vô hiệu hóa.");
        }

        // 3. Load the Rubric Version
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy Rubric Version.")
                );

        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException(
                    "Chỉ có thể cập nhật Rubric Version ở trạng thái DRAFT."
            );
        }

        // 4. Check that the Rubric belongs to the right school
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy bộ Rubric gốc.")
                );

        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL
                || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException(
                    "Rubric Version này không thuộc trường học của bạn."
            );
        }

        // 5. Validate the update data.

        String safeName = normalizeRequiredText(
                command.name(),
                "Tên phiên bản Rubric không được để trống."
        );

        // Empty (null or blank) is treated as unchanged -- keep the old description (SQL COALESCE),
        // consistent with UpdateSchoolRubricCriterionUseCase/UpdateSchoolRubricResultBandUseCase. It used
        // to use normalizeRequiredText(), which required a description, contradicting the "partial update"
        // comment right below for the other fields of the version.
        String safeDescription = (command.description() != null && !command.description().isBlank())
                ? StringNormalization.trimAndCollapseSpaces(command.description())
                : null;

        // Partial update: any field that is not passed (null) keeps the current value of the version
        // (updateRubricVersionAtomic uses COALESCE(:param, v.field) at the SQL layer).
        Instant finalEffectiveFrom = command.effectiveFrom() != null
                ? command.effectiveFrom() : version.getEffectiveFrom();

        Instant finalEffectiveTo = command.effectiveTo() != null
                ? command.effectiveTo() : version.getEffectiveTo();

        if (finalEffectiveTo != null
                && finalEffectiveFrom.isAfter(finalEffectiveTo)) {
            throw new IllegalArgumentException(
                    "Ngày bắt đầu hiệu lực không được lớn hơn ngày kết thúc hiệu lực."
            );
        }

        BigDecimal finalScoreMin = command.scoringScaleMin() != null
                ? command.scoringScaleMin() : version.getScoringScaleMin();

        BigDecimal finalScoreMax = command.scoringScaleMax() != null
                ? command.scoringScaleMax() : version.getScoringScaleMax();

        if (finalScoreMin.compareTo(finalScoreMax) > 0) {
            throw new IllegalArgumentException(
                    "Điểm tối thiểu không được lớn hơn điểm tối đa."
            );
        }

        // Block narrowing the scoring scale so that ResultBands/Criteria created earlier would fall outside the new scale
        rubricResultBandRepository.findByRubricVersionId(command.versionId()).forEach(band ->
                ScoreRangeValidator.assertWithinScale(finalScoreMin, finalScoreMax,
                        band.getScoreMin(), band.getScoreMax(), band.getName()));
        rubricCriterionRepository.findByRubricVersionId(command.versionId()).forEach(criterion ->
                ScoreRangeValidator.assertWithinScale(finalScoreMin, finalScoreMax,
                        criterion.getMinScore(), criterion.getMaxScore(), criterion.getName()));

        String safeMethod = command.totalScoreMethod() == null ? null : command.totalScoreMethod().name();
        // 6. Atomic update. For the fields that support partial updates (effectiveFrom/effectiveTo/
        // scoringScaleMin/scoringScaleMax), pass the original value (possibly null) straight from the command so the
        // SQL COALESCE keeps the value currently in the DB at the time of the update -- this avoids overwriting
        // with the `version` snapshot read earlier (a lost update when another request updates
        // these fields concurrently). finalEffectiveFrom/finalEffectiveTo/finalScoreMin/finalScoreMax are still used
        // above to validate business rules against the values after merging.
        try {
            rubricVersionRepository.updateRubricVersionAtomic(
                    command.versionId(),
                    null,
                    safeName,
                    safeDescription,
                    command.effectiveFrom(),
                    command.effectiveTo(),
                    command.scoringScaleMin(),
                    command.scoringScaleMax(),
                    safeMethod,
                    Instant.now(),
                    currentUserId.toString()
            );
        } catch (DataConflictException e) {
            throw new IllegalArgumentException(
                    "Không thể cập nhật Rubric Version do dữ liệu bị trùng hoặc không hợp lệ."
            );
        }

        return command.versionId();
    }

    private String normalizeRequiredText(String value, String errorMessage) {
        if (value == null) {
            throw new IllegalArgumentException(errorMessage);
        }

        String normalizedValue = StringNormalization.trimAndCollapseSpaces(value);

        if (normalizedValue.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }

        return normalizedValue;
    }
}
