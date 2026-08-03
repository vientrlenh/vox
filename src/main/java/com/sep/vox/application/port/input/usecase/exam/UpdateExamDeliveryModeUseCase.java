package com.sep.vox.application.port.input.usecase.exam;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.ExamDeliveryModeSupport;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamDeliveryModeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamDeliveryModeUseCase implements IUseCase<UpdateExamDeliveryModeCommand, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public UpdateExamDeliveryModeUseCase(
            ExamRepository examRepository,
            ExamPaperRepository examPaperRepository,
            ExamMemberRepository examMemberRepository,
            ExamCandidateRepository examCandidateRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examPaperRepository = examPaperRepository;
        this.examMemberRepository = examMemberRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamDto execute(UpdateExamDeliveryModeCommand input) {
        var deliveryMode = ExamDeliveryModeSupport.parse(input.deliveryMode());

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        authorize(exam, currentUserId, currentSchoolId, schoolAdmin);

        if (exam.getStatus() != ExamStatus.DRAFT && exam.getStatus() != ExamStatus.SCHEDULED) {
            throw new IllegalStateException("Không thể đổi hình thức làm bài khi bài kiểm tra đã bắt đầu hoặc kết thúc");
        }
        if (deliveryMode != exam.getDeliveryMode()
                && examCandidateRepository.existsByExamIdAndScheduleIdIsNotNull(exam.getId())) {
            throw new IllegalStateException(
                "Không thể đổi hình thức làm bài khi đã có thí sinh được xếp vào ca thi — hãy gỡ hết thí sinh khỏi ca thi trước");
        }

        exam.setDeliveryMode(deliveryMode);
        exam.setUpdatedAt(Instant.now());
        exam.setUpdatedBy(currentUserId);

        var saved = examRepository.save(exam);
        return ExamDtoMapper.toDto(saved, papersLocked(saved.getId()));
    }

    private void authorize(Exam exam, UUID currentUserId, UUID currentSchoolId, boolean schoolAdmin) {
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(exam.getSchoolId())) {
            return;
        }
        if (examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.CHAIR)) {
            return;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }

    private boolean papersLocked(UUID examId) {
        var papers = examPaperRepository.findByExamId(examId);
        return !papers.isEmpty()
            && papers.stream().allMatch(paper -> paper.getStatus() == ExamPaperStatus.LOCKED);
    }
}
