package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.ImportAssessmentCandidatesFromGradeCommand;
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
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
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
        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        var scope = assessmentDirectoryAccessService.resolve(assessment);
        // Nhập theo niên khóa là gom mọi lớp của niên khóa đó — vượt xa phạm vi của chủ
        // tịch một bài trên lớp. Chặn ở đây để quyền ghi khớp đúng quyền đọc
        // (`ViewAssessmentDirectoryGradesUseCase` từ chối cùng trường hợp này).
        if (!scope.schoolWide()) {
            throw new ForbiddenException("Bài kiểm tra trên lớp không hỗ trợ nhập thí sinh theo niên khóa");
        }
        var currentUserId = scope.callerId();
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        var grade = schoolGradeRepository.findById(input.schoolGradeId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khối"));
        if (!grade.getSchoolId().equals(assessment.getSchoolId())) {
            throw new ForbiddenException("Khối không thuộc trường của bài kiểm tra");
        }

        var now = Instant.now();
        var classes = schoolClassRepository.findBySchoolId(
            assessment.getSchoolId(), null, null, null, grade.getId(), 1, MAX_CLASSES).content();

        var activeUserIds = new LinkedHashSet<UUID>();
        for (var schoolClass : classes) {
            // findBySchoolClassId là 1-based (PageRequest.of(page - 1, size)) → trang đầu là 1, KHÔNG phải 0.
            var roster = schoolClassUserRepository.findBySchoolClassId(
                schoolClass.getId(), 1, MAX_CLASS_ROSTER_SIZE).content();
            for (var classUser : roster) {
                if (classUser.isActive()) {
                    activeUserIds.add(classUser.getUserId());
                }
            }
        }

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
        // Xem AddAssessmentCandidateUseCase: nhập cả khối vào bài đã publish thì số thí sinh -- một input
        // của ước lượng token đã soi lúc publish -- tăng lên mạnh nhất trong mọi đường thêm thí sinh,
        // càng phải soi lại ngay. Không cần check kind vì đường này chỉ chạy được cho scope trường
        // (CENTRALIZED) -- xem chặn ForbiddenException phía trên.
        if (assessment.getStatus() == AssessmentStatus.SCHEDULED) {
            classTestTokenQuotaGuardService.requireWithinTokenQuota(assessment);
        }
        return AssessmentCandidateDto.toDtoList(saved);
    }
}
