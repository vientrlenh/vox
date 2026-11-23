package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.service.rubric.RubricOrderValidator;
import com.sep.vox.domain.service.rubric.ScoreRangeValidator;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSystemRubricCriterionCommand;
import com.sep.vox.application.port.input.service.RubricCriterionExamplesNormalizer;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpdateSystemRubricCriterionUseCase implements IUseCase<UpdateSystemRubricCriterionCommand, UUID> {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final RubricCriterionExamplesNormalizer examplesNormalizer;

    @Override
    @Transactional
    public UUID execute(UpdateSystemRubricCriterionCommand command) {
        // 1. Xác thực tài khoản
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        // 2. Lấy Criterion
        RubricCriterion criterion = rubricCriterionRepository.findById(command.criterionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Tiêu chí (Criterion) này."));

        // 3. Lấy Version và kiểm tra DRAFT
        RubricVersion version = rubricVersionRepository.findById(criterion.getRubricVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Rubric Version chứa tiêu chí này."));
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ có thể sửa tiêu chí khi phiên bản Rubric đang ở trạng thái DRAFT.");
        }

        // 4. Lấy Rubric gốc và kiểm tra quyền SYSTEM
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Hành động bị từ chối: Tiêu chí này thuộc về Trường học.");
        }

        // 5. Chuẩn hóa & Validate Logic (Code không được phép sửa sau khi tạo, luôn giữ nguyên)
        String safeName = (command.name() != null && !command.name().isBlank()) ? StringNormalization.trimAndCollapseSpaces(command.name()) : null;
        String safeDesc = (command.description() != null && !command.description().isBlank()) ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;

        // Chặn 2026-08-14: xem lý do ở CreateSchoolRubricCriterionUseCase -- đường REST và đường
        // import file đang hiểu "20%" theo hai kiểu lệch nhau 100 lần.
        if (command.weight() != null
                && (command.weight().compareTo(BigDecimal.ZERO) < 0
                        || command.weight().compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException(
                    "Trọng số (weight) phải nằm trong khoảng 0 đến 1 -- lưu dưới dạng phân số,"
                            + " ví dụ 20% ghi là 0.2 và 100% ghi là 1.");
        }
        if (command.order() != null && command.order() <= 0) {
            throw new IllegalArgumentException("Thứ tự (order) phải lớn hơn 0.");
        }

        BigDecimal finalMinScore = command.minScore() != null ? command.minScore() : criterion.getMinScore();
        BigDecimal finalMaxScore = command.maxScore() != null ? command.maxScore() : criterion.getMaxScore();

        if (finalMinScore.compareTo(finalMaxScore) > 0) {
            throw new IllegalArgumentException("Điểm tối thiểu (minScore) không được lớn hơn điểm tối đa (maxScore).");
        }

        // Validate nằm trong thang điểm tổng của RubricVersion
        String nameForError = safeName != null ? safeName : criterion.getName();
        ScoreRangeValidator.assertWithinScale(version.getScoringScaleMin(), version.getScoringScaleMax(),
                finalMinScore, finalMaxScore, nameForError);

        // Validate không trùng thứ tự (order) với sibling khác trong cùng version
        if (command.order() != null) {
            Set<Integer> siblingOrders = rubricCriterionRepository.findByRubricVersionId(criterion.getRubricVersionId())
                    .stream()
                    .filter(c -> !c.getId().equals(criterion.getId()))
                    .map(c -> c.getOrder())
                    .collect(Collectors.toCollection(HashSet::new));
            RubricOrderValidator.assertNoDuplicateOrder(siblingOrders, command.order(), nameForError);
        }

        // =========================================================
        // Validate the examples JSON (guarded by the RubricCriterionExamples value object) and rewrite it in the {"values": [...]} shape before saving to the DB
        String examplesJsonToPersist = examplesNormalizer.normalize(command.examplesJson());

        // 6. Fire the atomic SQL update
        try {
            rubricCriterionRepository.updateCriterionAtomic(
                    command.criterionId(),
                    null,
                    safeName,
                    safeDesc,
                    examplesJsonToPersist,
                    command.weight(),
                    command.minScore(),
                    command.maxScore(),
                    command.order(),
                    command.isRequired(),
                    Instant.now(),
                    currentUserId.toString()
            );
        } catch (DataConflictException e) {
            throw new IllegalArgumentException("Không thể cập nhật Tiêu chí do dữ liệu bị trùng hoặc không hợp lệ.");
        }

        return command.criterionId();
    }
}