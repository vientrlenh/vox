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
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricResultBandCommand;
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
        requireActiveCurrentUser();

        RubricResultBand resultBand = rubricResultBandRepository.findById(command.resultBandId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Dải điểm Kết quả (Result Band) này."));
        RubricVersion version = loadDraftVersionOfSystemRubric(resultBand);

        // The code must not change after creation, so it is never updated.
        String safeName = (command.name() != null && !command.name().isBlank()) ? StringNormalization.trimAndCollapseSpaces(command.name()) : null;
        // A blank description keeps the stored one; an explicit null clears it.
        boolean descriptionBlank = command.description() != null && command.description().isBlank();
        boolean descriptionProvided = command.descriptionProvided() && !descriptionBlank;
        String safeDesc = (command.description() != null && !descriptionBlank) ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;

        if (command.order() != null && command.order() <= 0) {
            throw new IllegalArgumentException("Thứ tự (order) phải lớn hơn 0.");
        }
        requireValidRangeAndOrder(command, resultBand, version, safeName);

        updateResultBandAtomically(command, safeName, safeDesc, descriptionProvided);

        return command.resultBandId();
    }

    private void requireActiveCurrentUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }
    }

    // A ResultBand hangs directly off the Version, and that Version must be a DRAFT of a SYSTEM rubric.
    private RubricVersion loadDraftVersionOfSystemRubric(RubricResultBand resultBand) {
        RubricVersion version = rubricVersionRepository.findById(resultBand.getRubricVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Rubric Version chứa Dải điểm Kết quả này."));
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ có thể sửa Dải điểm Kết quả khi phiên bản Rubric đang ở trạng thái DRAFT.");
        }

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Hành động bị từ chối: Dải điểm Kết quả này thuộc về Trường học, không phải của Hệ thống.");
        }
        return version;
    }

    private void requireValidRangeAndOrder(UpdateSystemRubricResultBandCommand command, RubricResultBand resultBand,
                                           RubricVersion version, String safeName) {
        BigDecimal finalMinScore = command.scoreMin() != null ? command.scoreMin() : resultBand.getScoreMin();
        BigDecimal finalMaxScore = command.scoreMax() != null ? command.scoreMax() : resultBand.getScoreMax();

        if (finalMinScore.compareTo(finalMaxScore) > 0) {
            throw new IllegalArgumentException("Điểm sàn (scoreMin) không được lớn hơn điểm trần (scoreMax).");
        }

        List<RubricResultBand> siblingBands = rubricResultBandRepository.findByRubricVersionId(resultBand.getRubricVersionId())
                .stream()
                .filter(b -> !b.getId().equals(resultBand.getId()))
                .toList();
        String nameForError = safeName != null ? safeName : resultBand.getName();
        RubricResultBandValidator.assertNoOverlap(siblingBands, finalMinScore, finalMaxScore, nameForError);
        requireOrderNotTakenBySibling(command, siblingBands, nameForError);

        ScoreRangeValidator.assertWithinScale(version.getScoringScaleMin(), version.getScoringScaleMax(),
                finalMinScore, finalMaxScore, nameForError);
    }

    private void requireOrderNotTakenBySibling(UpdateSystemRubricResultBandCommand command,
                                               List<RubricResultBand> siblingBands, String nameForError) {
        if (command.order() != null) {
            Set<Integer> siblingOrders = siblingBands.stream()
                    .map(b -> b.getOrder())
                    .collect(Collectors.toCollection(HashSet::new));
            RubricOrderValidator.assertNoDuplicateOrder(siblingOrders, command.order(), nameForError);
        }
    }

    private void updateResultBandAtomically(UpdateSystemRubricResultBandCommand command, String safeName,
                                            String safeDesc, boolean descriptionProvided) {
        try {
            rubricResultBandRepository.updateResultBandAtomic(
                    command.resultBandId(),
                    null,
                    safeName,
                    safeDesc,
                    descriptionProvided,
                    command.scoreMin(),
                    command.scoreMax(),
                    command.order()
            );
        } catch (DataConflictException e) {
            throw new IllegalArgumentException("Không thể cập nhật Dải điểm Kết quả do dữ liệu bị trùng hoặc không hợp lệ.");
        }
    }
}
