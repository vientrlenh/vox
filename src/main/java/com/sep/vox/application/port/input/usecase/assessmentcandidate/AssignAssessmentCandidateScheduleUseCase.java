package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentcandidate.AssignAssessmentCandidateScheduleCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAutoAssigner;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.mapper.AssessmentCandidateDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;

@Service
@RequiredArgsConstructor
public class AssignAssessmentCandidateScheduleUseCase
        implements IUseCase<AssignAssessmentCandidateScheduleCommand, AssessmentCandidateDto> {

    private static final Set<AssessmentScheduleStatus> ASSIGNABLE_STATUSES =
        Set.of(AssessmentScheduleStatus.DRAFT, AssessmentScheduleStatus.PUBLISHED);

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentPaperAutoAssigner assessmentPaperAutoAssigner;
    private final AssessmentScheduleManageAccessService assessmentScheduleManageAccessService;
    private final AssessmentScheduleCandidateConflictValidator assessmentScheduleCandidateConflictValidator;

    @Override
    @Transactional
    public AssessmentCandidateDto execute(AssignAssessmentCandidateScheduleCommand input) {
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        UUID currentUserId = assessmentScheduleManageAccessService.requireCanManage(assessment);
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        AssessmentCandidate candidate = requireCandidateOf(assessment, input.candidateId());
        Instant now = Instant.now();

        // Removing the assignment from a schedule is always allowed, even when the candidate has no schedule (no-op).
        if (input.scheduleId() == null) {
            candidate.unassignFromSchedule(now, currentUserId);
            return AssessmentCandidateDtoMapper.toDto(assessmentCandidateRepository.save(candidate));
        }

        // Assigning the current schedule again short-circuits and skips the capacity check (avoids counting itself).
        if (input.scheduleId().equals(candidate.getScheduleId())) {
            return AssessmentCandidateDtoMapper.toDto(candidate);
        }

        return assignToSchedule(assessment, candidate, input.scheduleId(), now, currentUserId);
    }

    private AssessmentCandidate requireCandidateOf(Assessment assessment, UUID candidateId) {
        AssessmentCandidate candidate = assessmentCandidateRepository.findById(candidateId)
            .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        if (!candidate.getAssessmentId().equals(assessment.getId())) {
            throw new ResourceNotFoundException("Candidate not found");
        }
        return candidate;
    }

    private AssessmentCandidateDto assignToSchedule(
        Assessment assessment, AssessmentCandidate candidate, UUID scheduleId, Instant now, UUID currentUserId) {
        AssessmentSchedule schedule = requireAssignableSchedule(assessment, scheduleId);
        // The candidate's own current schedule is excluded by the validator, so changing schedule inside the same
        // assessment (even when the two schedules overlap) still works.
        assessmentScheduleCandidateConflictValidator.requireCandidatesFree(
            List.of(candidate), schedule.getStartDate(), schedule.getEndDate());

        candidate.assignToSchedule(schedule.getId(), now, currentUserId);
        // Assigns the default paper right away if every paper variant is locked, so that no extra paper-assignment click is needed. If papers
        // are not locked yet it is left empty, and AssessmentPaperAutoAssigner.backfillAssessment will assign when the last paper variant is locked.
        assessmentPaperAutoAssigner.assignPapersIfNeeded(assessment, List.of(candidate), now, currentUserId);
        return AssessmentCandidateDtoMapper.toDto(assessmentCandidateRepository.save(candidate));
    }
    private AssessmentSchedule requireAssignableSchedule(Assessment assessment, UUID scheduleId) {
        AssessmentSchedule schedule = assessmentScheduleRepository.findByIdForUpdate(scheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Assessment schedule not found"));
        if (!schedule.getAssessmentId().equals(assessment.getId())) {
            throw new ResourceNotFoundException("Assessment schedule not found");
        }
        if (!ASSIGNABLE_STATUSES.contains(schedule.getStatus())) {
            throw new IllegalArgumentException("Candidates can only be placed in an assessment schedule that is a draft or published");
        }
        return schedule;
    }
}
