package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSystemRubricCriterionBandCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UpdateSystemRubricCriterionBandUseCase implements IUseCase<UpdateSystemRubricCriterionBandCommand, UUID> {

    private final RubricCriterionBandRepository rubricCriterionBandRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public UpdateSystemRubricCriterionBandUseCase(
            RubricCriterionBandRepository rubricCriterionBandRepository,
            RubricCriterionRepository rubricCriterionRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricCriterionBandRepository = rubricCriterionBandRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSystemRubricCriterionBandCommand command) {
        // 1. Xác thực User
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        // 2. Lấy Band
        var band = rubricCriterionBandRepository.findById(command.bandId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Dải điểm (Band) này."));

        // 3. Lấy Criterion
        var criterion = rubricCriterionRepository.findById(band.getCriterionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Tiêu chí chứa Dải điểm này."));

        // 4. Lấy Version và kiểm tra DRAFT
        var version = rubricVersionRepository.findById(criterion.getRubricVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric Version."));
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể sửa Dải điểm khi phiên bản Rubric đang ở trạng thái DRAFT.");
        }

        // 5. Lấy Rubric gốc và kiểm tra quyền SYSTEM
        var rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Hành động bị từ chối: Dải điểm này thuộc về Trường học, không phải của Hệ thống.");
        }

        // 6. Chuẩn hóa & Validate Logic
        String safeCode = (command.code() != null && !command.code().isBlank()) ? StringNormalization.trimAndCollapseSpaces(command.code()) : null;

        // Check chéo scoreMin <= scoreMax
        BigDecimal finalMinScore = command.scoreMin() != null ? command.scoreMin() : band.getScoreMin();
        BigDecimal finalMaxScore = command.scoreMax() != null ? command.scoreMax() : band.getScoreMax();

        if (finalMinScore.compareTo(finalMaxScore) > 0) {
            throw new IllegalArgumentException("Điểm sàn (scoreMin) không được lớn hơn điểm trần (scoreMax).");
        }

        // 7. Bắn SQL Atomic Update
        try {
            rubricCriterionBandRepository.updateBandAtomic(
                    command.bandId(),
                    safeCode,
                    command.scoreMin(),
                    command.scoreMax(),
                    OffsetDateTime.now(),
                    currentUserId
            );
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Mã dải điểm (Code) này đã tồn tại trong Tiêu chí hiện tại. Vui lòng chọn mã khác.");
        }

        return command.bandId();
    }
}