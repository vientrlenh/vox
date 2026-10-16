package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.AssignAssessmentCandidateScheduleCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAutoAssigner;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;

@Service
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

    public AssignAssessmentCandidateScheduleUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentPaperAutoAssigner assessmentPaperAutoAssigner,
            AssessmentScheduleManageAccessService assessmentScheduleManageAccessService,
            AssessmentScheduleCandidateConflictValidator assessmentScheduleCandidateConflictValidator) {
        this.assessmentPaperAutoAssigner = assessmentPaperAutoAssigner;
        this.assessmentRepository = assessmentRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentScheduleManageAccessService = assessmentScheduleManageAccessService;
        this.assessmentScheduleCandidateConflictValidator = assessmentScheduleCandidateConflictValidator;
    }

    @Override
    @Transactional
    public AssessmentCandidateDto execute(AssignAssessmentCandidateScheduleCommand input) {
        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = assessmentScheduleManageAccessService.requireCanManage(assessment);
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        var candidate = assessmentCandidateRepository.findById(input.candidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thí sinh"));
        if (!candidate.getAssessmentId().equals(assessment.getId())) {
            throw new ResourceNotFoundException("Không tìm thấy thí sinh");
        }

        var now = Instant.now();

        // Bỏ gán khỏi ca luôn được phép, kể cả khi thí sinh đã không có ca (no-op).
        if (input.scheduleId() == null) {
            candidate.unassignFromSchedule(now, currentUserId);
            return AssessmentCandidateDto.toDto(assessmentCandidateRepository.save(candidate));
        }

        // Gán lại đúng ca hiện tại: short-circuit, không kiểm tra lại sức chứa (tránh tự-đếm-chính-mình).
        if (input.scheduleId().equals(candidate.getScheduleId())) {
            return AssessmentCandidateDto.toDto(candidate);
        }

        var schedule = assessmentScheduleRepository.findByIdForUpdate(input.scheduleId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi"));
        if (!schedule.getAssessmentId().equals(assessment.getId())) {
            throw new ResourceNotFoundException("Không tìm thấy ca thi");
        }
        if (!ASSIGNABLE_STATUSES.contains(schedule.getStatus())) {
            throw new IllegalStateException("Chỉ có thể xếp thí sinh vào ca ở trạng thái nháp hoặc đã công bố");
        }

        // Ca hiện tại của chính thí sinh này được validator tự loại, nên đổi ca trong cùng một kỳ
        // thi (kể cả khi hai ca chồng giờ) vẫn chạy được.
        assessmentScheduleCandidateConflictValidator.requireCandidatesFree(
            List.of(candidate), schedule.getStartDate(), schedule.getEndDate());

        candidate.assignToSchedule(schedule.getId(), now, currentUserId);
        // Gán đề mặc định luôn nếu mọi mã đề đã khoá, để không phải bấm thêm bước phân đề. Đề chưa
        // khoá thì bỏ trống, AssessmentPaperAutoAssigner.backfillAssessment sẽ gán khi mã đề cuối cùng được khoá.
        assessmentPaperAutoAssigner.assignPapersIfNeeded(assessment, List.of(candidate), now, currentUserId);
        return AssessmentCandidateDto.toDto(assessmentCandidateRepository.save(candidate));
    }
}
