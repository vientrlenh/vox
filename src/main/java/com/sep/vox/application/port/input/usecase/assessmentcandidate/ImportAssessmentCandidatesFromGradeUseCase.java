package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.school.SchoolGrade;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ImportAssessmentCandidatesFromGradeCommand;
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
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;

@Service
public class ImportAssessmentCandidatesFromGradeUseCase
        implements IUseCase<ImportAssessmentCandidatesFromGradeCommand, List<AssessmentCandidateDto>> {

    private static final int MAX_CLASSES = 200;
    private static final int MAX_CLASS_ROSTER_SIZE = 1000;

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final AssessmentDirectoryAccessService assessmentDirectoryAccessService;
    private final ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService;

    public ImportAssessmentCandidatesFromGradeUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            SchoolGradeRepository schoolGradeRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            AssessmentDirectoryAccessService assessmentDirectoryAccessService,
            ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.assessmentDirectoryAccessService = assessmentDirectoryAccessService;
        this.classTestTokenQuotaGuardService = classTestTokenQuotaGuardService;
    }

    @Override
    @Transactional
    public List<AssessmentCandidateDto> execute(ImportAssessmentCandidatesFromGradeCommand input) {
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        AssessmentDirectoryAccessService.AssessmentDirectoryScope scope = assessmentDirectoryAccessService.resolve(assessment);
        // Importing by school year gathers every class of that school year -- far beyond the scope of the chair
        // of a class test. Blocked here so that write permission matches read permission
        // (`ViewAssessmentDirectoryGradesUseCase` rejects the same case).
        if (!scope.schoolWide()) {
            throw new ForbiddenException("Bài kiểm tra trên lớp không hỗ trợ nhập thí sinh theo niên khóa");
        }
        UUID currentUserId = scope.callerId();
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        SchoolGrade grade = schoolGradeRepository.findById(input.schoolGradeId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy khối"));
        if (!grade.getSchoolId().equals(assessment.getSchoolId())) {
            throw new ForbiddenException("Khối không thuộc trường của bài kiểm tra");
        }

        Instant now = Instant.now();
        List<SchoolClass> classes = schoolClassRepository.findBySchoolId(
            assessment.getSchoolId(), null, null, null, grade.getId(), 1, MAX_CLASSES).content();

        Set<UUID> activeUserIds = new LinkedHashSet<UUID>();
        for (SchoolClass schoolClass : classes) {
            // findBySchoolClassId là 1-based (PageRequest.of(page - 1, size)) → trang đầu là 1, KHÔNG phải 0.
            List<SchoolClassUser> roster = schoolClassUserRepository.findBySchoolClassId(
                schoolClass.getId(), 1, MAX_CLASS_ROSTER_SIZE).content();
            for (SchoolClassUser classUser : roster) {
                if (classUser.isActive()) {
                    activeUserIds.add(classUser.getUserId());
                }
            }
        }

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
        // See AddAssessmentCandidateUseCase: importing a whole grade into a published assessment makes the number of candidates -- an input
        // to the token estimate checked at publish time -- grow the most of any path that adds candidates, so
        // it has to be re-checked right away. There is no need to check kind because this path can only run for the school scope
        // (CENTRALIZED) -- see the ForbiddenException block above.
        if (assessment.getStatus() == AssessmentStatus.SCHEDULED) {
            classTestTokenQuotaGuardService.requireWithinTokenQuota(assessment);
        }
        return AssessmentCandidateDtoMapper.toDtoList(saved);
    }
}
