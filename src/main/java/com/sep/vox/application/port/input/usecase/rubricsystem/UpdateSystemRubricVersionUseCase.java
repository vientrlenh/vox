package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSystemRubricVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UpdateSystemRubricVersionUseCase
        implements IUseCase<UpdateSystemRubricVersionCommand, UUID> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public UpdateSystemRubricVersionUseCase(
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort
    ) {
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSystemRubricVersionCommand command) {
        // 1. Xác thực tài khoản System Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        // 2. Lấy Rubric Version và kiểm tra trạng thái
        var version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() ->
                        new NotFoundException("Không tìm thấy Rubric Version.")
                );

        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException(
                    "Chỉ có thể cập nhật thông tin khi phiên bản đang ở trạng thái DRAFT."
            );
        }

        // 3. Kiểm tra Rubric phải thuộc SYSTEM
        var rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() ->
                        new NotFoundException("Không tìm thấy bộ Rubric gốc.")
                );

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException(
                    "Hành động bị từ chối: Rubric này thuộc về Trường học."
            );
        }

        //4. Chuẩn hóa và validate dữ liệu.


        String safeName = normalizeRequiredText(
                command.name(),
                "Tên phiên bản Rubric không được để trống."
        );

        String safeDescription = normalizeRequiredText(
                command.description(),
                "Mô tả phiên bản Rubric không được để trống."
        );

        OffsetDateTime finalEffectiveFrom = command.effectiveFrom();
        if (finalEffectiveFrom == null) {
            throw new IllegalArgumentException(
                    "Ngày bắt đầu hiệu lực không được để trống."
            );
        }

        OffsetDateTime finalEffectiveTo = command.effectiveTo();

        if (finalEffectiveTo != null
                && finalEffectiveFrom.isAfter(finalEffectiveTo)) {
            throw new IllegalArgumentException(
                    "Ngày bắt đầu hiệu lực không được lớn hơn ngày kết thúc hiệu lực."
            );
        }

        BigDecimal finalScoreMin = command.scoringScaleMin();
        if (finalScoreMin == null) {
            throw new IllegalArgumentException(
                    "Điểm tối thiểu không được để trống."
            );
        }

        BigDecimal finalScoreMax = command.scoringScaleMax();
        if (finalScoreMax == null) {
            throw new IllegalArgumentException(
                    "Điểm tối đa không được để trống."
            );
        }

        if (finalScoreMin.compareTo(finalScoreMax) > 0) {
            throw new IllegalArgumentException(
                    "Điểm tối thiểu không được lớn hơn điểm tối đa."
            );
        }

        String safeMethod = parseRequiredTotalScoreMethod(
                command.totalScoreMethod()
        );

        // 5. Update atomic: truyền null cho code để giữ nguyên code cũ.
        try {
            rubricVersionRepository.updateRubricVersionAtomic(
                    command.versionId(),
                    null,
                    safeName,
                    safeDescription,
                    finalEffectiveFrom,
                    finalEffectiveTo,
                    finalScoreMin,
                    finalScoreMax,
                    safeMethod,
                    OffsetDateTime.now(),
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

    private String parseRequiredTotalScoreMethod(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Phương pháp tính tổng điểm không được để trống."
            );
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