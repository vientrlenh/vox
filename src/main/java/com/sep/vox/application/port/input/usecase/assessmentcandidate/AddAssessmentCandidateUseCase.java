package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import com.sep.vox.domain.model.assessment.Assessment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AddAssessmentCandidateCommand;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.mapper.AssessmentCandidateDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
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
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        AssessmentDirectoryAccessService.AssessmentDirectoryScope scope = assessmentDirectoryAccessService.resolve(assessment);
        UUID currentUserId = scope.callerId();
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        // Thí sinh phải là school_user vai trò STUDENT cùng trường với bài kiểm tra.
        if (!schoolUserRepository.existsBySchoolIdAndUserId(assessment.getSchoolId(), input.studentId())) {
            throw new IllegalArgumentException("Học sinh không thuộc trường của bài kiểm tra");
        }
        // The chair of a class test can only add students of the class they are in charge of -- exactly matching
        // the scope that `ViewAssessmentDirectoryStudentsUseCase` lets them see.
        if (!scope.schoolWide() && !isInCallerClasses(scope, input.studentId())) {
            throw new ForbiddenException("Học sinh không thuộc lớp bạn phụ trách");
        }
        boolean isStudent = userRoleQueryRepository.findByUserIdWithRoleInfo(input.studentId()).stream()
            .anyMatch(role -> SchoolRoleCodes.STUDENT.equals(role.roleCode()));
        if (!isStudent) {
            throw new IllegalArgumentException("Người dùng không phải là học sinh");
        }

        if (assessmentCandidateRepository.existsByAssessmentIdAndStudentId(assessment.getId(), input.studentId())) {
            throw new DuplicatedException("Thí sinh đã tồn tại trong kỳ thi này");
        }

        AssessmentCandidate candidate = AssessmentCandidate.createFresh(assessment.getId(), input.studentId(), currentUserId,
            Instant.now());
        AssessmentCandidate saved = assessmentCandidateRepository.save(candidate);
        // Bài đã publish (SCHEDULED) mà thêm học sinh thì số thí sinh -- một input của ước lượng
        // token đã soi lúc publish -- tăng lên; soi lại ngay, không riêng CLASS_TEST, không để tới
        // lúc chấm xong mới vỡ quota (xem ClassTestTokenQuotaGuardService).
        if (assessment.getStatus() == AssessmentStatus.SCHEDULED) {
            classTestTokenQuotaGuardService.requireWithinTokenQuota(assessment);
        }
        return AssessmentCandidateDtoMapper.toDto(saved);
    }

    private boolean isInCallerClasses(AssessmentDirectoryAccessService.AssessmentDirectoryScope scope, UUID studentId) {
        List<UUID> callerClassIds = assessmentDirectoryAccessService.callerClassIds(scope);
        if (callerClassIds.isEmpty()) {
            return false;
        }
        return schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(List.of(studentId), callerClassIds)
            .stream()
            .anyMatch(classUser -> classUser.isActive());
    }
}
