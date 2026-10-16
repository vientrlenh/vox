package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.AddAssessmentCandidateCommand;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.mapper.AssessmentCandidateDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;

@Service
public class AddAssessmentCandidateUseCase implements IUseCase<AddAssessmentCandidateCommand, AssessmentCandidateDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final AssessmentDirectoryAccessService assessmentDirectoryAccessService;
    private final ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;

    public AddAssessmentCandidateUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            AssessmentDirectoryAccessService assessmentDirectoryAccessService,
            ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.assessmentDirectoryAccessService = assessmentDirectoryAccessService;
        this.classTestTokenQuotaGuardService = classTestTokenQuotaGuardService;
    }

    @Override
    @Transactional
    public AssessmentCandidateDto execute(AddAssessmentCandidateCommand input) {
        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        var scope = assessmentDirectoryAccessService.resolve(assessment);
        var currentUserId = scope.callerId();
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        // Thí sinh phải là school_user vai trò STUDENT cùng trường với bài kiểm tra.
        if (!schoolUserRepository.existsBySchoolIdAndUserId(assessment.getSchoolId(), input.studentId())) {
            throw new IllegalArgumentException("Học sinh không thuộc trường của bài kiểm tra");
        }
        // Chủ tịch một bài trên lớp chỉ thêm được học sinh trong lớp mình phụ trách — khớp
        // đúng phạm vi mà `ViewAssessmentDirectoryStudentsUseCase` cho họ nhìn thấy.
        if (!scope.schoolWide() && !isInCallerClasses(scope, input.studentId())) {
            throw new ForbiddenException("Học sinh không thuộc lớp bạn phụ trách");
        }
        var isStudent = userRoleQueryRepository.findByUserIdWithRoleInfo(input.studentId()).stream()
            .anyMatch(role -> SchoolRoleCodes.STUDENT.equals(role.roleCode()));
        if (!isStudent) {
            throw new IllegalArgumentException("Người dùng không phải là học sinh");
        }

        if (assessmentCandidateRepository.existsByAssessmentIdAndStudentId(assessment.getId(), input.studentId())) {
            throw new ResourceDuplicatedException("Thí sinh đã tồn tại trong kỳ thi này");
        }

        var candidate = AssessmentCandidate.createFresh(assessment.getId(), input.studentId(), currentUserId,
            Instant.now());
        var saved = assessmentCandidateRepository.save(candidate);
        // Bài đã publish (SCHEDULED) mà thêm học sinh thì số thí sinh -- một input của ước lượng
        // token đã soi lúc publish -- tăng lên; soi lại ngay, không riêng CLASS_TEST, không để tới
        // lúc chấm xong mới vỡ quota (xem ClassTestTokenQuotaGuardService).
        if (assessment.getStatus() == AssessmentStatus.SCHEDULED) {
            classTestTokenQuotaGuardService.requireWithinTokenQuota(assessment);
        }
        return AssessmentCandidateDtoMapper.toDto(saved);
    }

    private boolean isInCallerClasses(AssessmentDirectoryAccessService.AssessmentDirectoryScope scope, UUID studentId) {
        var callerClassIds = assessmentDirectoryAccessService.callerClassIds(scope);
        if (callerClassIds.isEmpty()) {
            return false;
        }
        return schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(List.of(studentId), callerClassIds)
            .stream()
            .anyMatch(classUser -> classUser.isActive());
    }
}
