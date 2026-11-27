package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import com.sep.vox.domain.model.assessment.Assessment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentcandidate.AddAssessmentCandidateCommand;
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
@RequiredArgsConstructor
public class AddAssessmentCandidateUseCase implements IUseCase<AddAssessmentCandidateCommand, AssessmentCandidateDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final AssessmentDirectoryAccessService assessmentDirectoryAccessService;
    private final ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;

    @Override
    @Transactional
    public AssessmentCandidateDto execute(AddAssessmentCandidateCommand input) {
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        AssessmentDirectoryAccessService.AssessmentDirectoryScope scope = assessmentDirectoryAccessService.resolve(assessment);
        UUID currentUserId = scope.callerId();
        AssessmentEditingGuard.requireScheduleEditable(assessment);
        requireAddableStudent(assessment, scope, input.studentId());

        if (assessmentCandidateRepository.existsByAssessmentIdAndStudentId(assessment.getId(), input.studentId())) {
            throw new ResourceDuplicatedException("The candidate already exists in this assessment");
        }

        AssessmentCandidate candidate = AssessmentCandidate.createFresh(assessment.getId(), input.studentId(), currentUserId,
            Instant.now());
        AssessmentCandidate saved = assessmentCandidateRepository.save(candidate);
        requireTokenQuotaWhenScheduled(assessment);
        return AssessmentCandidateDtoMapper.toDto(saved);
    }

    private void requireAddableStudent(
        Assessment assessment, AssessmentDirectoryAccessService.AssessmentDirectoryScope scope, UUID studentId) {
        // The candidate must be a school_user with the STUDENT role in the same school as the assessment.
        if (!schoolUserRepository.existsBySchoolIdAndUserId(assessment.getSchoolId(), studentId)) {
            throw new IllegalArgumentException("The student does not belong to the school of the assessment");
        }
        // The chair of a class test can only add students of the class they are in charge of -- exactly matching
        // the scope that `ViewAssessmentDirectoryStudentsUseCase` lets them see.
        if (!scope.schoolWide() && !isInCallerClasses(scope, studentId)) {
            throw new ForbiddenException("The student is not in a class you are in charge of");
        }
        boolean isStudent = userRoleQueryRepository.findByUserIdWithRoleInfo(studentId).stream()
            .anyMatch(role -> SchoolRoleCodes.STUDENT.equals(role.roleCode()));
        if (!isStudent) {
            throw new IllegalArgumentException("The user is not a student");
        }
    }

    /**
     * Adding a student to a published (SCHEDULED) assessment raises the number of candidates, one input of the token
     * estimate checked at publish time. It is checked again at once, not only for CLASS_TEST, so the quota does not
     * break only when grading finishes (see ClassTestTokenQuotaGuardService).
     */
    private void requireTokenQuotaWhenScheduled(Assessment assessment) {
        if (assessment.getStatus() == AssessmentStatus.SCHEDULED) {
            classTestTokenQuotaGuardService.requireWithinTokenQuota(assessment);
        }
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
