package com.sep.vox.application.port.input.usecase.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ConflictException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamDeliveryModeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamDeliveryMode;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamDeliveryModeUseCase implements IUseCase<UpdateExamDeliveryModeCommand, ExamDto> {

    private final ExamRepository examRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public UpdateExamDeliveryModeUseCase(
            ExamRepository examRepository,
            ExamPaperRepository examPaperRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examPaperRepository = examPaperRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamDto execute(UpdateExamDeliveryModeCommand input) {
        var deliveryMode = parseDeliveryMode(input.deliveryMode());

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        authorize(exam, currentUserId, currentSchoolId, schoolAdmin);

        if (exam.getKind() != ExamKind.CLASS_TEST) {
            throw new ConflictException("Hình thức làm bài chỉ áp dụng cho bài kiểm tra trên lớp");
        }
        if (exam.getStatus() != ExamStatus.DRAFT && exam.getStatus() != ExamStatus.SCHEDULED) {
            throw new ConflictException("Không thể đổi hình thức làm bài khi bài kiểm tra đã bắt đầu hoặc kết thúc");
        }

        exam.setDeliveryMode(deliveryMode);
        exam.setUpdatedAt(OffsetDateTime.now());
        exam.setUpdatedBy(currentUserId);

        var saved = examRepository.save(exam);
        return ExamDtoMapper.toDto(saved, papersLocked(saved.getId()));
    }

    private ExamDeliveryMode parseDeliveryMode(String value) {
        try {
            return ExamDeliveryMode.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Hình thức làm bài không hợp lệ");
        }
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
