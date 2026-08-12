package com.sep.vox.application.port.input.usecase.examcandidate;

import java.time.Instant;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AssignExamCandidateScheduleCommand;
import com.sep.vox.application.port.input.service.ClassTestPaperAutoAssigner;
import com.sep.vox.application.port.input.service.ExamScheduleManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.domain.mapper.ExamCandidateDtoMapper;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.service.exam.ExamEditingGuard;

@Service
public class AssignExamCandidateScheduleUseCase
        implements IUseCase<AssignExamCandidateScheduleCommand, ExamCandidateDto> {

    private static final Set<ExamScheduleStatus> ASSIGNABLE_STATUSES =
        Set.of(ExamScheduleStatus.DRAFT, ExamScheduleStatus.PUBLISHED);

    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ClassTestPaperAutoAssigner classTestPaperAutoAssigner;
    private final ExamScheduleManageAccessService examScheduleManageAccessService;

    public AssignExamCandidateScheduleUseCase(
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamScheduleRepository examScheduleRepository,
            ClassTestPaperAutoAssigner classTestPaperAutoAssigner,
            ExamScheduleManageAccessService examScheduleManageAccessService) {
        this.classTestPaperAutoAssigner = classTestPaperAutoAssigner;
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examScheduleManageAccessService = examScheduleManageAccessService;
    }

    @Override
    @Transactional
    public ExamCandidateDto execute(AssignExamCandidateScheduleCommand input) {
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = examScheduleManageAccessService.requireCanManage(exam);
        ExamEditingGuard.requireScheduleEditable(exam);

        var candidate = examCandidateRepository.findById(input.candidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh"));
        if (!candidate.getExamId().equals(exam.getId())) {
            throw new NotFoundException("Không tìm thấy thí sinh");
        }

        var now = Instant.now();

        // Bỏ gán khỏi ca luôn được phép, kể cả khi thí sinh đã không có ca (no-op).
        if (input.scheduleId() == null) {
            candidate.unassignFromSchedule(now, currentUserId);
            return ExamCandidateDtoMapper.toDto(examCandidateRepository.save(candidate));
        }

        // Gán lại đúng ca hiện tại: short-circuit, không kiểm tra lại sức chứa (tránh tự-đếm-chính-mình).
        if (input.scheduleId().equals(candidate.getScheduleId())) {
            return ExamCandidateDtoMapper.toDto(candidate);
        }

        var schedule = examScheduleRepository.findByIdForUpdate(input.scheduleId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
        if (!schedule.getExamId().equals(exam.getId())) {
            throw new NotFoundException("Không tìm thấy ca thi");
        }
        if (!ASSIGNABLE_STATUSES.contains(schedule.getStatus())) {
            throw new IllegalStateException("Chỉ có thể xếp thí sinh vào ca ở trạng thái nháp hoặc đã công bố");
        }

        candidate.assignToSchedule(schedule.getId(), now, currentUserId);
        // Bài kiểm tra trên lớp chỉ có một đề nên gán luôn, giáo viên không phải bấm thêm bước phân đề.
        classTestPaperAutoAssigner.assignSinglePaperIfNeeded(exam, candidate, now, currentUserId);
        return ExamCandidateDtoMapper.toDto(examCandidateRepository.save(candidate));
    }
}
