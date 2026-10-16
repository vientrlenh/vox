package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.ImportAssessmentCandidatesFromClassCommand;
import com.sep.vox.application.port.input.service.ClassTestTokenQuotaGuardService;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;

@Service
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

    public ImportAssessmentCandidatesFromClassUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            AssessmentDirectoryAccessService assessmentDirectoryAccessService,
            ClassTestTokenQuotaGuardService classTestTokenQuotaGuardService) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.assessmentDirectoryAccessService = assessmentDirectoryAccessService;
        this.classTestTokenQuotaGuardService = classTestTokenQuotaGuardService;
    }

    @Override
    @Transactional
    public List<AssessmentCandidateDto> execute(ImportAssessmentCandidatesFromClassCommand input) {
        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        var scope = assessmentDirectoryAccessService.resolve(assessment);
        var currentUserId = scope.callerId();
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        var schoolClass = schoolClassRepository.findById(input.schoolClassId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
        if (!schoolClass.getSchoolId().equals(assessment.getSchoolId())) {
            throw new ForbiddenException("Lớp học không thuộc trường của bài kiểm tra");
        }
        // Chủ tịch một bài trên lớp chỉ nhập được lớp mình phụ trách — khớp đúng phạm vi
        // mà `ViewAssessmentDirectoryClassesUseCase` cho họ nhìn thấy.
        if (!scope.schoolWide()
                && !assessmentDirectoryAccessService.callerClassIds(scope).contains(schoolClass.getId())) {
            throw new ForbiddenException("Bạn không phụ trách lớp học này");
        }

        var now = Instant.now();
        // findBySchoolClassId là 1-based (PageRequest.of(page - 1, size)) → trang đầu là 1, KHÔNG phải 0.
        var roster = schoolClassUserRepository.findBySchoolClassId(
            schoolClass.getId(), 1, MAX_CLASS_ROSTER_SIZE).content();

        var activeUserIds = roster.stream()
            .filter(user -> user.isActive())
            .map(user -> user.getUserId())
            .distinct()
            .toList();

        var studentIds = userRoleQueryRepository.findUserIdsByRoleCode(activeUserIds, SchoolRoleCodes.STUDENT);
        var existingStudentIds = assessmentCandidateRepository.findStudentIdsByAssessmentId(assessment.getId());

        var newCandidates = new ArrayList<AssessmentCandidate>();
        for (var userId : activeUserIds) {
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
        var saved = assessmentCandidateRepository.saveAll(newCandidates);
        // Xem AddAssessmentCandidateUseCase: thêm cả loạt thí sinh vào bài đã publish thì càng phải soi lại
        // token quota ngay, không riêng CLASS_TEST, không đợi tới lúc chấm xong mới vỡ.
        if (assessment.getStatus() == AssessmentStatus.SCHEDULED) {
            classTestTokenQuotaGuardService.requireWithinTokenQuota(assessment);
        }
        return AssessmentCandidateDto.toDtoList(saved);
    }
}
