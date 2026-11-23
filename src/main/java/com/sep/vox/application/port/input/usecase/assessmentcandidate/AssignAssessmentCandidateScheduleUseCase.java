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

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AssignAssessmentCandidateScheduleCommand;
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
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        UUID currentUserId = assessmentScheduleManageAccessService.requireCanManage(assessment);
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        AssessmentCandidate candidate = assessmentCandidateRepository.findById(input.candidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh"));
        if (!candidate.getAssessmentId().equals(assessment.getId())) {
            throw new NotFoundException("Không tìm thấy thí sinh");
        }

        Instant now = Instant.now();

        // Bỏ gán khỏi ca luôn được phép, kể cả khi thí sinh đã không có ca (no-op).
        if (input.scheduleId() == null) {
            candidate.unassignFromSchedule(now, currentUserId);
            return AssessmentCandidateDtoMapper.toDto(assessmentCandidateRepository.save(candidate));
        }

        // Gán lại đúng ca hiện tại: short-circuit, không kiểm tra lại sức chứa (tránh tự-đếm-chính-mình).
        if (input.scheduleId().equals(candidate.getScheduleId())) {
            return AssessmentCandidateDtoMapper.toDto(candidate);
        }

        AssessmentSchedule schedule = assessmentScheduleRepository.findByIdForUpdate(input.scheduleId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
        if (!schedule.getAssessmentId().equals(assessment.getId())) {
            throw new NotFoundException("Không tìm thấy ca thi");
        }
        if (!ASSIGNABLE_STATUSES.contains(schedule.getStatus())) {
            throw new IllegalStateException("Chỉ có thể xếp thí sinh vào ca ở trạng thái nháp hoặc đã công bố");
        }

        // Ca hiện tại của chính thí sinh này được validator tự loại, nên đổi ca trong cùng một kỳ
        // thi (kể cả khi hai ca chồng giờ) vẫn chạy được.
        assessmentScheduleCandidateConflictValidator.requireCandidatesFree(
            List.of(candidate), schedule.getStartDate(), schedule.getEndDate());

        candidate.assignToSchedule(schedule.getId(), now, currentUserId);
        // Assigns the default paper right away if every paper variant is locked, so that no extra paper-assignment click is needed. If papers
        // are not locked yet it is left empty, and AssessmentPaperAutoAssigner.backfillAssessment will assign when the last paper variant is locked.
        assessmentPaperAutoAssigner.assignPapersIfNeeded(assessment, List.of(candidate), now, currentUserId);
        return AssessmentCandidateDtoMapper.toDto(assessmentCandidateRepository.save(candidate));
    }
}
