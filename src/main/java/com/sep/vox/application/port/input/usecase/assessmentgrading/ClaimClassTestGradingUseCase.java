package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ClaimClassTestGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingRoundPolicy;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;

/**
 * The teacher who created a class test claims the grading themself -- without going through the school.
 *
 * <p>Placed in the class test facade instead of loosening the {@code @PreAuthorize} of
 * {@code GradingAssignmentController}: loosening that would open the door to <em>every</em> teacher
 * on <em>every</em> assessment, while here the scope is closed to exactly the tests where the caller is the CHAIR.
 *
 * <p>The {@code APPEAL} round is NOT claimed through here: it is tied to a specific appeal so it
 * must go through {@code AssignAssessmentAppealReviewerUseCase} -- the only place that knows the conflict-of-interest
 * rule and how to move the appeal's status.
 *
 * <p>Validates everything before writing: a wrong row must not leave a half-done assignment from
 * the rows before it.
 */
@Service
public class ClaimClassTestGradingUseCase
        implements IUseCase<ClaimClassTestGradingCommand, List<UUID>> {

    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    public ClaimClassTestGradingUseCase(
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository,
            AssessmentGradingAccessService assessmentGradingAccessService) {
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentGradingAssignmentRepository = assessmentGradingAssignmentRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
    }

    @Override
    @Transactional
    public List<UUID> execute(ClaimClassTestGradingCommand command) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        assessmentGradingAccessService.authorizeClassTestChair(command.assessmentId(), currentUserId);

        GradingRoundType roundType = requireNotAppealRound(command.roundType());
        List<UUID> candidateResultIds = command.candidateResultIds() == null
            ? List.<UUID>of() : command.candidateResultIds();
        if (candidateResultIds.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất một bài để nhận chấm.");
        }
        if (new HashSet<>(candidateResultIds).size() != candidateResultIds.size()) {
            throw new DuplicatedException("Không được chọn trùng bài.");
        }

        Map<UUID, AssessmentCandidateResult> resultsById = assessmentCandidateResultRepository.findByIdIn(candidateResultIds).stream()
            .collect(Collectors.toMap(result -> result.getId(), Function.identity(), (left, right) -> left));

        // Một query cho cả lô — pre-check để báo lỗi tiếng Việt thay vì để unique index
        // ném DataIntegrityViolation; index vẫn là chốt cuối khi có race.
        Set<UUID> alreadyOpen = assessmentGradingAssignmentRepository
            .findOpenByCandidateResultIdIn(candidateResultIds).stream()
            .map(assignment -> assignment.getCandidateResultId())
            .collect(Collectors.toSet());

        Instant now = Instant.now();
        List<AssessmentGradingAssignment> assignments = new java.util.ArrayList<AssessmentGradingAssignment>();
        for (UUID candidateResultId : candidateResultIds) {
            AssessmentCandidateResult result = resultsById.get(candidateResultId);
            if (result == null) {
                throw new NotFoundException("Không tìm thấy kết quả bài thi.");
            }
            // Blocks claiming an attempt of another test -- the assessmentId in the URL is the thing that has passed the permission check.
            if (!command.assessmentId().equals(result.getAssessmentId())) {
                throw new NotFoundException("Bài thi không thuộc bài kiểm tra này.");
            }
            if (!GradingRoundPolicy.isAssignable(roundType, result.getStatus())) {
                throw new IllegalStateException(
                    "Bài thi không ở trạng thái phù hợp với vòng chấm " + roundType.name() + ".");
            }
            if (alreadyOpen.contains(candidateResultId)) {
                throw new DuplicatedException("Bài thi này đang được chấm.");
            }

            // scoreBefore chụp NGAY LÚC NHẬN — mốc đo độ lệch so với vòng trước.
            assignments.add(AssessmentGradingAssignment.open(
                candidateResultId,
                currentUserId,
                roundType,
                null,
                result.getTotalScore(),
                now,
                currentUserId,
                null
            ));
        }

        return assessmentGradingAssignmentRepository.saveAll(assignments).stream()
            .map(assignment -> assignment.getId())
            .toList();
    }

    private GradingRoundType requireNotAppealRound(GradingRoundType roundType) {
        if (roundType == GradingRoundType.APPEAL) {
            throw new IllegalArgumentException(
                "Vòng phúc khảo được nhận từ màn đơn phúc khảo, không nhận ở đây.");
        }
        return roundType;
    }
}
