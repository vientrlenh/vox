package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentgrading.ClaimClassTestGradingCommand;
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
 * {@code RestGradingAssignmentController}: loosening that would open the door to <em>every</em> teacher
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
@RequiredArgsConstructor
public class ClaimClassTestGradingUseCase
        implements IUseCase<ClaimClassTestGradingCommand, List<UUID>> {

    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    @Override
    @Transactional
    public List<UUID> execute(ClaimClassTestGradingCommand command) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        assessmentGradingAccessService.authorizeClassTestChair(command.assessmentId(), currentUserId);

        GradingRoundType roundType = requireNotAppealRound(command.roundType());
        List<UUID> candidateResultIds = requireCandidateResultIds(command.candidateResultIds());
        List<AssessmentGradingAssignment> assignments =
            buildClaims(command.assessmentId(), roundType, candidateResultIds, currentUserId);

        return assessmentGradingAssignmentRepository.saveAll(assignments).stream()
            .map(assignment -> assignment.getId())
            .toList();
    }

    private List<UUID> requireCandidateResultIds(List<UUID> requestedIds) {
        List<UUID> candidateResultIds = requestedIds == null ? List.<UUID>of() : requestedIds;
        if (candidateResultIds.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất một bài để nhận chấm.");
        }
        if (new HashSet<>(candidateResultIds).size() != candidateResultIds.size()) {
            throw new ResourceDuplicatedException("Không được chọn trùng bài.");
        }
        return candidateResultIds;
    }

    private List<AssessmentGradingAssignment> buildClaims(
            UUID assessmentId, GradingRoundType roundType, List<UUID> candidateResultIds, UUID currentUserId) {
        Map<UUID, AssessmentCandidateResult> resultsById = assessmentCandidateResultRepository.findByIdIn(candidateResultIds).stream()
            .collect(Collectors.toMap(result -> result.getId(), Function.identity(), (left, right) -> left));
        Set<UUID> alreadyOpen = openAssignedResultIds(candidateResultIds);

        Instant now = Instant.now();
        List<AssessmentGradingAssignment> assignments = new ArrayList<AssessmentGradingAssignment>();
        for (UUID candidateResultId : candidateResultIds) {
            AssessmentCandidateResult result = requireClaimable(
                assessmentId, roundType, resultsById.get(candidateResultId), alreadyOpen.contains(candidateResultId));
            assignments.add(openClaim(result, candidateResultId, roundType, currentUserId, now));
        }
        return assignments;
    }

    /**
     * One query for the whole batch -- a pre-check so the error message is in Vietnamese instead of letting the
     * unique index throw DataIntegrityViolation; the index remains the last safeguard when there is a race.
     */
    private Set<UUID> openAssignedResultIds(List<UUID> candidateResultIds) {
        return assessmentGradingAssignmentRepository
            .findOpenByCandidateResultIdIn(candidateResultIds).stream()
            .map(assignment -> assignment.getCandidateResultId())
            .collect(Collectors.toSet());
    }

    private AssessmentCandidateResult requireClaimable(
            UUID assessmentId, GradingRoundType roundType, AssessmentCandidateResult result, boolean alreadyOpen) {
        if (result == null) {
            throw new ResourceNotFoundException("Không tìm thấy kết quả bài thi.");
        }
        // Blocks claiming an attempt of another test -- the assessmentId in the URL is the thing that has passed the permission check.
        if (!assessmentId.equals(result.getAssessmentId())) {
            throw new ResourceNotFoundException("Bài thi không thuộc bài kiểm tra này.");
        }
        if (!GradingRoundPolicy.isAssignable(roundType, result.getStatus())) {
            throw new IllegalArgumentException(
                "Bài thi không ở trạng thái phù hợp với vòng chấm " + roundType.name() + ".");
        }
        if (alreadyOpen) {
            throw new ResourceDuplicatedException("Bài thi này đang được chấm.");
        }
        return result;
    }

    /** scoreBefore is captured RIGHT AT CLAIM -- the reference point for measuring the deviation from the previous round. */
    private AssessmentGradingAssignment openClaim(
            AssessmentCandidateResult result, UUID candidateResultId, GradingRoundType roundType,
            UUID currentUserId, Instant now) {
        return AssessmentGradingAssignment.open(
            candidateResultId,
            currentUserId,
            roundType,
            null,
            result.getTotalScore(),
            now,
            currentUserId,
            null
        );
    }

    private GradingRoundType requireNotAppealRound(GradingRoundType roundType) {
        if (roundType == GradingRoundType.APPEAL) {
            throw new IllegalArgumentException(
                "Vòng phúc khảo được nhận từ màn đơn phúc khảo, không nhận ở đây.");
        }
        return roundType;
    }
}
