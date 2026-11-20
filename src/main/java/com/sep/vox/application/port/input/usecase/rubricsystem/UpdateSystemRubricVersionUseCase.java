package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.service.rubric.ScoreRangeValidator;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSystemRubricVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class UpdateSystemRubricVersionUseCase
        implements IUseCase<UpdateSystemRubricVersionCommand, UUID> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricCriterionRepository rubricCriterionRepository;

    public UpdateSystemRubricVersionUseCase(
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            RubricResultBandRepository rubricResultBandRepository,
            RubricCriterionRepository rubricCriterionRepository
    ) {
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSystemRubricVersionCommand command) {
        // 1. Authenticate the System Admin account
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        // 2. Load the Rubric Version and check its status
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() ->
                        new NotFoundException("Không tìm thấy Rubric Version.")
                );

        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException(
                    "Chỉ có thể cập nhật thông tin khi phiên bản đang ở trạng thái DRAFT."
            );
        }

        // 3. Check that the Rubric belongs to SYSTEM
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() ->
                        new NotFoundException("Không tìm thấy bộ Rubric gốc.")
                );

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException(
                    "Hành động bị từ chối: Rubric này thuộc về Trường học."
            );
        }

        // 4. Normalize and validate the data.


        String safeName = normalizeRequiredText(
                command.name(),
                "Tên phiên bản Rubric không được để trống."
        );

        // Empty (null or blank) is treated as unchanged -- keep the old description (SQL COALESCE),
        // consistent with UpdateSystemRubricCriterionUseCase/UpdateSystemRubricResultBandUseCase. It used
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
        // 5. Atomic update: pass null for the code to keep the old code. For the fields that support
        // partial updates (effectiveFrom/effectiveTo/scoringScaleMin/scoringScaleMax), pass the original value
        // (possibly null) straight from the command so the SQL COALESCE keeps the value currently in the DB at the
        // time of the update -- this avoids overwriting with the `version` snapshot read earlier (a lost update when
        // another request updates these fields concurrently). finalEffectiveFrom/finalEffectiveTo/finalScoreMin/
        // finalScoreMax are still used above to validate business rules against the values after merging.
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
                    currentUserId
            );
        } catch (DataIntegrityViolationException e) {
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
