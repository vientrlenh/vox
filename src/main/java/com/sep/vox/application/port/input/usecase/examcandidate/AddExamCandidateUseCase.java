package com.sep.vox.application.port.input.usecase.examcandidate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.ExamEditingGuard;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AddExamCandidateCommand;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.ExamDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.domain.mapper.ExamCandidateDtoMapper;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class AddExamCandidateUseCase implements IUseCase<AddExamCandidateCommand, ExamCandidateDto> {

    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final ExamDirectoryAccessService examDirectoryAccessService;
    private final ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;

    public AddExamCandidateUseCase(
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            ExamDirectoryAccessService examDirectoryAccessService,
            ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService) {
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.examDirectoryAccessService = examDirectoryAccessService;
        this.classTestTokenQuotaGuardService = classTestTokenQuotaGuardService;
    }

    @Override
    @Transactional
    public ExamCandidateDto execute(AddExamCandidateCommand input) {
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var scope = examDirectoryAccessService.resolve(exam);
        var currentUserId = scope.callerId();
        ExamEditingGuard.requireScheduleEditable(exam);

        // Thí sinh phải là school_user vai trò STUDENT cùng trường với bài kiểm tra.
        if (!schoolUserRepository.existsBySchoolIdAndUserId(exam.getSchoolId(), input.studentId())) {
            throw new IllegalArgumentException("Học sinh không thuộc trường của bài kiểm tra");
        }
        // Chủ tịch một bài trên lớp chỉ thêm được học sinh trong lớp mình phụ trách — khớp
        // đúng phạm vi mà `ViewExamDirectoryStudentsUseCase` cho họ nhìn thấy.
        if (!scope.schoolWide() && !isInCallerClasses(scope, input.studentId())) {
            throw new ForbiddenException("Học sinh không thuộc lớp bạn phụ trách");
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
        var saved = examCandidateRepository.save(candidate);
        // Bài trên lớp đã publish (SCHEDULED) mà thêm học sinh thì số thí sinh -- một input của ước
        // lượng token đã soi lúc publish -- tăng lên; soi lại ngay, không để tới lúc chấm xong mới vỡ
        // quota (xem ClassTestTokenQuotaGuardService).
        if (exam.getKind() == ExamKind.CLASS_TEST && exam.getStatus() == ExamStatus.SCHEDULED) {
            classTestTokenQuotaGuardService.requireWithinTokenQuota(exam);
        }
        return ExamCandidateDtoMapper.toDto(saved);
    }

    private boolean isInCallerClasses(ExamDirectoryAccessService.ExamDirectoryScope scope, UUID studentId) {
        var callerClassIds = examDirectoryAccessService.callerClassIds(scope);
        if (callerClassIds.isEmpty()) {
            return false;
        }
        return schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(List.of(studentId), callerClassIds)
            .stream()
            .anyMatch(classUser -> classUser.isActive());
    }
}
