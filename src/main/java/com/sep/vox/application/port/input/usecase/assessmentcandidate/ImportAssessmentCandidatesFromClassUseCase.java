package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentcandidate.ImportAssessmentCandidatesFromClassCommand;
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
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;

@Service
@RequiredArgsConstructor
public class ImportAssessmentCandidatesFromClassUseCase
        implements IUseCase<ImportAssessmentCandidatesFromClassCommand, List<AssessmentCandidateDto>> {

    private static final int MAX_CLASS_ROSTER_SIZE = 1000;

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final AssessmentDirectoryAccessService assessmentDirectoryAccessService;
    private final ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;

    @Override
    @Transactional
    public List<AssessmentCandidateDto> execute(ImportAssessmentCandidatesFromClassCommand input) {
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        AssessmentDirectoryAccessService.AssessmentDirectoryScope scope = assessmentDirectoryAccessService.resolve(assessment);
        UUID currentUserId = scope.callerId();
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        SchoolClass schoolClass = requireImportableClass(assessment, scope, input.schoolClassId());

        Instant now = Instant.now();
        List<UUID> activeUserIds = findActiveRosterUserIds(schoolClass);
        List<AssessmentCandidate> newCandidates = buildNewStudentCandidates(
            assessment, activeUserIds, currentUserId, now);

        if (newCandidates.isEmpty()) {
            return List.of();
        }
        List<AssessmentCandidate> saved = assessmentCandidateRepository.saveAll(newCandidates);
        requireTokenQuotaWhenScheduled(assessment);
        return AssessmentCandidateDtoMapper.toDtoList(saved);
    }

    private SchoolClass requireImportableClass(
        Assessment assessment, AssessmentDirectoryAccessService.AssessmentDirectoryScope scope, UUID schoolClassId) {
        SchoolClass schoolClass = schoolClassRepository.findById(schoolClassId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        if (!schoolClass.getSchoolId().equals(assessment.getSchoolId())) {
            throw new ForbiddenException("The class does not belong to the school of the assessment");
        }
        // The chair of a class test can only import the class they are in charge of -- exactly matching the scope
        // that ViewAssessmentDirectoryClassesUseCase lets them see.
        if (!scope.schoolWide()
                && !assessmentDirectoryAccessService.callerClassIds(scope).contains(schoolClass.getId())) {
            throw new ForbiddenException("You are not in charge of this class");
        }
        return schoolClass;
    }

    private List<UUID> findActiveRosterUserIds(SchoolClass schoolClass) {
        // findBySchoolClassId is 1-based (PageRequest.of(page - 1, size)), so the first page is 1, NOT 0.
        List<SchoolClassUser> roster = schoolClassUserRepository.findBySchoolClassId(
            schoolClass.getId(), 1, MAX_CLASS_ROSTER_SIZE).content();

        return roster.stream()
            .filter(user -> user.isActive())
            .map(user -> user.getUserId())
            .distinct()
            .toList();
    }

    private List<AssessmentCandidate> buildNewStudentCandidates(
        Assessment assessment, List<UUID> activeUserIds, UUID currentUserId, Instant now) {
        Set<UUID> studentIds = userRoleQueryRepository.findUserIdsByRoleCode(activeUserIds, SchoolRoleCodes.STUDENT);
        Set<UUID> existingStudentIds = assessmentCandidateRepository.findStudentIdsByAssessmentId(assessment.getId());

        List<AssessmentCandidate> newCandidates = new ArrayList<AssessmentCandidate>();
        for (UUID userId : activeUserIds) {
            if (!studentIds.contains(userId)) {
                continue;
            }
            if (existingStudentIds.contains(userId)) {
                continue;
            }
            newCandidates.add(AssessmentCandidate.createFresh(assessment.getId(), userId, currentUserId, now));
        }
        return newCandidates;
    }

    /**
     * See AddAssessmentCandidateUseCase: adding a whole batch of candidates to a published assessment makes it even
     * more necessary to re-check the token quota immediately, not only for CLASS_TEST, rather than waiting until
     * grading finishes for it to blow up.
     */
    private void requireTokenQuotaWhenScheduled(Assessment assessment) {
        if (assessment.getStatus() == AssessmentStatus.SCHEDULED) {
            classTestTokenQuotaGuardService.requireWithinTokenQuota(assessment);
        }
    }
}
