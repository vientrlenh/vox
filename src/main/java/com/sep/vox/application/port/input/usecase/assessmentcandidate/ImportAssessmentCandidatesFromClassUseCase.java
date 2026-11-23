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
import com.sep.vox.application.port.input.command.ImportAssessmentCandidatesFromClassCommand;
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
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        AssessmentDirectoryAccessService.AssessmentDirectoryScope scope = assessmentDirectoryAccessService.resolve(assessment);
        UUID currentUserId = scope.callerId();
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        SchoolClass schoolClass = schoolClassRepository.findById(input.schoolClassId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
        if (!schoolClass.getSchoolId().equals(assessment.getSchoolId())) {
            throw new ForbiddenException("Lớp học không thuộc trường của bài kiểm tra");
        }
        // The chair of a class test can only import the class they are in charge of -- exactly matching the scope
        // that `ViewAssessmentDirectoryClassesUseCase` lets them see.
        if (!scope.schoolWide()
                && !assessmentDirectoryAccessService.callerClassIds(scope).contains(schoolClass.getId())) {
            throw new ForbiddenException("Bạn không phụ trách lớp học này");
        }

        Instant now = Instant.now();
        // findBySchoolClassId là 1-based (PageRequest.of(page - 1, size)) → trang đầu là 1, KHÔNG phải 0.
        List<SchoolClassUser> roster = schoolClassUserRepository.findBySchoolClassId(
            schoolClass.getId(), 1, MAX_CLASS_ROSTER_SIZE).content();

        List<UUID> activeUserIds = roster.stream()
            .filter(user -> user.isActive())
            .map(user -> user.getUserId())
            .distinct()
            .toList();

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

        if (newCandidates.isEmpty()) {
            return List.of();
        }
        List<AssessmentCandidate> saved = assessmentCandidateRepository.saveAll(newCandidates);
        // See AddAssessmentCandidateUseCase: adding a whole batch of candidates to a published assessment makes it even more necessary to re-check
        // the token quota immediately, not only for CLASS_TEST, rather than waiting until grading finishes for it to blow up.
        if (assessment.getStatus() == AssessmentStatus.SCHEDULED) {
            classTestTokenQuotaGuardService.requireWithinTokenQuota(assessment);
        }
        return AssessmentCandidateDtoMapper.toDtoList(saved);
    }
}
