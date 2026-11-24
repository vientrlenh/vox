package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.service.rubric.RubricOrderValidator;
import com.sep.vox.domain.service.rubric.ScoreRangeValidator;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricCriterionCommand;
import com.sep.vox.application.port.input.service.RubricCriterionExamplesNormalizer;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpdateSchoolRubricCriterionUseCase implements IUseCase<UpdateSchoolRubricCriterionCommand, UUID> {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final RubricCriterionExamplesNormalizer examplesNormalizer;

    @Override
    @Transactional
    public UUID execute(UpdateSchoolRubricCriterionCommand command) {
        // 1. Authenticate the User
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        // 2. Security checkpoint via SchoolUser
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản chưa liên kết với trường học."));
        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền sửa dữ liệu của trường khác.");
        }

        School school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Trường học này đang bị vô hiệu hóa.");
        }

        // 3. Load the Criterion
        RubricCriterion criterion = rubricCriterionRepository.findById(command.criterionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Tiêu chí (Criterion) này."));

        // 4. Load the Version and check it is DRAFT
        RubricVersion version = rubricVersionRepository.findById(criterion.getRubricVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Rubric Version chứa tiêu chí này."));
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ có thể sửa tiêu chí khi phiên bản Rubric đang ở trạng thái DRAFT.");
        }

        // 5. Load the original Rubric and check the SCHOOL permission (make sure the Criterion belongs to the school's Rubric)
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Hành động bị từ chối: Tiêu chí này không thuộc về trường học của bạn.");
        }

        // 6. Normalize & validate the data (the code must not change after creation and always stays as it is)
        String safeName = (command.name() != null && !command.name().isBlank()) ? StringNormalization.trimAndCollapseSpaces(command.name()) : null;
        // A blank description keeps the stored one; an explicit null clears it.
        boolean descriptionBlank = command.description() != null && command.description().isBlank();
        boolean descriptionProvided = command.descriptionProvided() && !descriptionBlank;
        String safeDesc = (command.description() != null && !descriptionBlank) ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;

        // Guard added 2026-08-14: see the reason in CreateSchoolRubricCriterionUseCase -- the REST path and the
        // file-import path read "20%" in two ways that differ by a factor of 100.
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

        // Validate that it lies within the total score scale of the RubricVersion
        String nameForError = safeName != null ? safeName : criterion.getName();
        ScoreRangeValidator.assertWithinScale(version.getScoringScaleMin(), version.getScoringScaleMax(),
                finalMinScore, finalMaxScore, nameForError);

        // Validate that the order does not clash with another sibling in the same version
        if (command.order() != null) {
            Set<Integer> siblingOrders = rubricCriterionRepository.findByRubricVersionId(criterion.getRubricVersionId())
                    .stream()
                    .filter(c -> !c.getId().equals(criterion.getId()))
                    .map(c -> c.getOrder())
                    .collect(Collectors.toCollection(HashSet::new));
            RubricOrderValidator.assertNoDuplicateOrder(siblingOrders, command.order(), nameForError);
        }

        // 7. Validate the examples JSON (guarded by the RubricCriterionExamples value object) and rewrite it in the {"values": [...]} shape before saving to the DB
        String examplesJsonToPersist = examplesNormalizer.normalize(command.examplesJson());

        // 8. Fire the atomic SQL update
        try {
            rubricCriterionRepository.updateCriterionAtomic(
                    command.criterionId(),
                    null,
                    safeName,
                    safeDesc,
                    descriptionProvided,
                    examplesJsonToPersist,
                    command.examplesJsonProvided(),
                    command.weight(),
                    command.minScore(),
                    command.maxScore(),
                    command.order(),
                    command.isRequired()
            );
        } catch (DataConflictException e) {
            throw new IllegalArgumentException("Không thể cập nhật Tiêu chí do dữ liệu bị trùng hoặc không hợp lệ.");
        }

        return command.criterionId();
    }
}