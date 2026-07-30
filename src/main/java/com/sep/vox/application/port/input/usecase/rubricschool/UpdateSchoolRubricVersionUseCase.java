package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.domain.service.rubric.ScoreRangeValidator;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSchoolRubricVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
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

    public UpdateSchoolRubricVersionUseCase(
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SchoolRepository schoolRepository,
            SchoolUserRepository schoolUserRepository,
            RubricResultBandRepository rubricResultBandRepository,
            RubricCriterionRepository rubricCriterionRepository
    ) {
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.schoolRepository = schoolRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSchoolRubricVersionCommand command) {
        // 1. Xác thực tài khoản
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }

        // 2. Kiểm tra user thuộc đúng trường
        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() ->
                        new ForbiddenException("Tài khoản chưa được liên kết với trường học.")
                );

        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException(
                    "Không có quyền cập nhật dữ liệu của trường học khác."
            );
        }

        var school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() ->
                        new NotFoundException("Không tìm thấy trường học.")
                );

        if (!school.isActive()) {
            throw new ForbiddenException("Trường học này đang bị vô hiệu hóa.");
        }

        // 3. Lấy Rubric Version
        var version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() ->
                        new NotFoundException("Không tìm thấy Rubric Version.")
                );

        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException(
                    "Chỉ có thể cập nhật Rubric Version ở trạng thái DRAFT."
            );
        }

        // 4. Kiểm tra Rubric thuộc đúng trường
        var rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() ->
                        new NotFoundException("Không tìm thấy bộ Rubric gốc.")
                );

        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL
                || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException(
                    "Rubric Version này không thuộc trường học của bạn."
            );
        }

        //5. Validate dữ liệu update.

        String safeName = normalizeRequiredText(
                command.name(),
                "Tên phiên bản Rubric không được để trống."
        );

        String safeDescription = normalizeRequiredText(
                command.description(), "Mô tả phiên bản Rubric không được để trống."
        );

        // Cập nhật một phần: field nào không được truyền (null) thì giữ nguyên giá trị hiện tại của version
        // (updateRubricVersionAtomic dùng COALESCE(:param, v.field) ở tầng SQL).
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

        // Chặn thu hẹp thang điểm làm ResultBand/Criterion đã tạo trước đó bị lọt ra ngoài thang mới
        rubricResultBandRepository.findByRubricVersionId(command.versionId()).forEach(band ->
                ScoreRangeValidator.assertWithinScale(finalScoreMin, finalScoreMax,
                        band.getScoreMin(), band.getScoreMax(), band.getName()));
        rubricCriterionRepository.findByRubricVersionId(command.versionId()).forEach(criterion ->
                ScoreRangeValidator.assertWithinScale(finalScoreMin, finalScoreMax,
                        criterion.getMinScore(), criterion.getMaxScore(), criterion.getName()));

        String safeMethod = parseOptionalTotalScoreMethod(
                command.totalScoreMethod()
        );
        // 6. Update atomic. Với các field hỗ trợ cập nhật một phần (effectiveFrom/effectiveTo/
        // scoringScaleMin/scoringScaleMax), truyền thẳng giá trị gốc (có thể null) từ command để tầng
        // SQL COALESCE tự giữ nguyên giá trị hiện có trong DB tại thời điểm update — tránh việc ghi đè
        // bằng snapshot `version` đã đọc trước đó (lost update khi có request khác cập nhật đồng thời
        // các field này). finalEffectiveFrom/finalEffectiveTo/finalScoreMin/finalScoreMax vẫn được dùng
        // ở trên để validate business rule dựa trên giá trị sau khi merge.
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

    private String parseOptionalTotalScoreMethod(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return RubricTotalScoreMethod.valueOf(value.trim()).name();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Phương pháp tính tổng điểm không hợp lệ. Chỉ nhận SUM hoặc WEIGHTED_AVERAGE."
            );
        }
    }
}
