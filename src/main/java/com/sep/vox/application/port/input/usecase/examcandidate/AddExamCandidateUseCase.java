package com.sep.vox.application.port.input.usecase.examcandidate;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AddExamCandidateCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.domain.mapper.ExamCandidateDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class AddExamCandidateUseCase implements IUseCase<AddExamCandidateCommand, ExamCandidateDto> {

    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public AddExamCandidateUseCase(
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamCandidateDto execute(AddExamCandidateCommand input) {
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = authorize(exam);

        // Thí sinh phải là school_user vai trò STUDENT cùng trường với bài kiểm tra.
        if (!schoolUserRepository.existsBySchoolIdAndUserId(exam.getSchoolId(), input.studentId())) {
            throw new IllegalArgumentException("Học sinh không thuộc trường của bài kiểm tra");
        }
        var isStudent = userRoleQueryRepository.findByUserIdWithRoleInfo(input.studentId()).stream()
            .anyMatch(role -> SchoolRoleCodes.STUDENT.equals(role.roleCode()));
        if (!isStudent) {
            throw new IllegalArgumentException("Người dùng không phải là học sinh");
        }

        if (examCandidateRepository.existsByExamIdAndStudentId(exam.getId(), input.studentId())) {
            throw new DuplicatedException("Thí sinh đã tồn tại trong kỳ thi này");
        }

        var candidate = ExamCandidate.createFresh(exam.getId(), input.studentId(), currentUserId,
            Instant.now());
        return ExamCandidateDtoMapper.toDto(examCandidateRepository.save(candidate));
    }

    private UUID authorize(Exam exam) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(exam.getSchoolId())) {
            return currentUserId;
        }
        if (examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.CHAIR)) {
            return currentUserId;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
