package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.service.rubric.RubricOrderValidator;
import com.sep.vox.domain.service.rubric.RubricResultBandValidator;
import com.sep.vox.domain.service.rubric.ScoreRangeValidator;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSystemRubricResultBandCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpdateSystemRubricResultBandUseCase implements IUseCase<UpdateSystemRubricResultBandCommand, UUID> {

    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(UpdateSystemRubricResultBandCommand command) {
        // 1. Xác thực User
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        // 2. Lấy Result Band
        RubricResultBand resultBand = rubricResultBandRepository.findById(command.resultBandId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Dải điểm Kết quả (Result Band) này."));

        // 3. Lấy Version và kiểm tra DRAFT (ResultBand nối thẳng với Version)
        RubricVersion version = rubricVersionRepository.findById(resultBand.getRubricVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Rubric Version chứa Dải điểm Kết quả này."));
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ có thể sửa Dải điểm Kết quả khi phiên bản Rubric đang ở trạng thái DRAFT.");
        }

        // 4. Lấy Rubric gốc và kiểm tra quyền SYSTEM
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Hành động bị từ chối: Dải điểm Kết quả này thuộc về Trường học, không phải của Hệ thống.");
        }

        // 5. Chuẩn hóa & Validate Logic (Code không được phép sửa sau khi tạo, luôn giữ nguyên)
        String safeName = (command.name() != null && !command.name().isBlank()) ? StringNormalization.trimAndCollapseSpaces(command.name()) : null;
        String safeDesc = (command.description() != null && !command.description().isBlank()) ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;

        if (command.order() != null && command.order() <= 0) {
            throw new IllegalArgumentException("Thứ tự (order) phải lớn hơn 0.");
        }

        // Check chéo scoreMin <= scoreMax
        BigDecimal finalMinScore = command.scoreMin() != null ? command.scoreMin() : resultBand.getScoreMin();
        BigDecimal finalMaxScore = command.scoreMax() != null ? command.scoreMax() : resultBand.getScoreMax();

        if (finalMinScore.compareTo(finalMaxScore) > 0) {
            throw new IllegalArgumentException("Điểm sàn (scoreMin) không được lớn hơn điểm trần (scoreMax).");
        }

        // Validate không chồng lấn với các band khác trong cùng version
        List<RubricResultBand> siblingBands = rubricResultBandRepository.findByRubricVersionId(resultBand.getRubricVersionId())
                .stream()
                .filter(b -> !b.getId().equals(resultBand.getId()))
                .toList();
        String nameForError = safeName != null ? safeName : resultBand.getName();
        RubricResultBandValidator.assertNoOverlap(siblingBands, finalMinScore, finalMaxScore, nameForError);

        // Validate không trùng thứ tự (order) với sibling khác trong cùng version
        if (command.order() != null) {
            Set<Integer> siblingOrders = siblingBands.stream()
                    .map(b -> b.getOrder())
                    .collect(Collectors.toCollection(HashSet::new));
            RubricOrderValidator.assertNoDuplicateOrder(siblingOrders, command.order(), nameForError);
        }

        // Validate nằm trong thang điểm tổng của RubricVersion
        ScoreRangeValidator.assertWithinScale(version.getScoringScaleMin(), version.getScoringScaleMax(),
                finalMinScore, finalMaxScore, nameForError);

        // 6. Fire the atomic SQL update
        try {
            rubricResultBandRepository.updateResultBandAtomic(
                    command.resultBandId(),
                    null,
                    safeName,
                    safeDesc,
                    command.scoreMin(),
                    command.scoreMax(),
                    command.order(),
                    Instant.now(),
                    currentUserId.toString()
            );
        } catch (DataConflictException e) {
            throw new IllegalArgumentException("Không thể cập nhật Dải điểm Kết quả do dữ liệu bị trùng hoặc không hợp lệ.");
        }

        return command.resultBandId();
    }
}