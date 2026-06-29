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
public class UpdateSystemRubricVersionUseCase implements IUseCase<UpdateSystemRubricVersionCommand, UUID> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public UpdateSystemRubricVersionUseCase(
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSystemRubricVersionCommand command) {
        // 1. Xác thực tài khoản (Dùng hàm exists để tối ưu SQL như nãy bàn)
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        // 2. Lấy Version lên và kiểm tra DRAFT
        var version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric Version."));

        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể cập nhật thông tin khi phiên bản đang ở trạng thái DRAFT.");
        }

        // 3. Kiểm tra quyền Hệ thống (SYSTEM)
        var rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Hành động bị từ chối: Rubric này thuộc về Trường học.");
        }

        // 4. Chuẩn hóa & Validate Data (Atomic)
        String safeCode = (command.code() != null && !command.code().isBlank())
                ? StringNormalization.trimAndCollapseSpaces(command.code()) : null;
        String safeName = (command.name() != null && !command.name().isBlank())
                ? StringNormalization.trimAndCollapseSpaces(command.name()) : null;
        String safeDesc = (command.description() != null && !command.description().isBlank())
                ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;

        OffsetDateTime finalEffectiveFrom = command.effectiveFrom() != null ? command.effectiveFrom() : version.getEffectiveFrom();
        OffsetDateTime finalEffectiveTo = command.effectiveTo() != null ? command.effectiveTo() : version.getEffectiveTo();

        if (finalEffectiveTo != null && finalEffectiveFrom.isAfter(finalEffectiveTo)) {
            throw new IllegalArgumentException("Ngày bắt đầu hiệu lực (EffectiveFrom) không được lớn hơn ngày kết thúc (EffectiveTo).");
        }

        BigDecimal finalScoreMin = command.scoringScaleMin() != null ? command.scoringScaleMin() : version.getScoringScaleMin();
        BigDecimal finalScoreMax = command.scoringScaleMax() != null ? command.scoringScaleMax() : version.getScoringScaleMax();

        if (finalScoreMin.compareTo(finalScoreMax) > 0) {
            throw new IllegalArgumentException("Điểm sàn (Min) không được lớn hơn điểm trần (Max).");
        }

        String safeMethod = null;
        if (command.totalScoreMethod() != null) {
            try {
                safeMethod = RubricTotalScoreMethod.valueOf(command.totalScoreMethod()).name();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Phương pháp tính tổng điểm không hợp lệ (Chỉ nhận WEIGHTED_AVERAGE hoặc SUM).");
            }
        }

        // 5.(Bọc try-catch chống trùng CODE)
        try {
            rubricVersionRepository.updateRubricVersionAtomic(
                    command.versionId(),
                    safeCode,
                    safeName,
                    safeDesc,
                    command.effectiveFrom(),
                    command.effectiveTo(),
                    command.scoringScaleMin(),
                    command.scoringScaleMax(),
                    safeMethod,
                    OffsetDateTime.now(),
                    currentUserId
            );
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Mã phiên bản (Code) này đã tồn tại trong bộ Rubric. Vui lòng chọn mã khác.");
        }

        return command.versionId();
    }
}