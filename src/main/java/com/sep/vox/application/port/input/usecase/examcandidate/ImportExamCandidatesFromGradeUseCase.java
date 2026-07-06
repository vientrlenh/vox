package com.sep.vox.application.port.input.usecase.examcandidate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ImportExamCandidatesFromGradeCommand;
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
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class ImportExamCandidatesFromGradeUseCase
        implements IUseCase<ImportExamCandidatesFromGradeCommand, List<ExamCandidateDto>> {

    private static final int MAX_CLASSES = 200;
    private static final int MAX_CLASS_ROSTER_SIZE = 1000;

    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolGradeLevelRepository schoolGradeLevelRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public ImportExamCandidatesFromGradeUseCase(
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            SchoolGradeRepository schoolGradeRepository,
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public List<ExamCandidateDto> execute(ImportExamCandidatesFromGradeCommand input) {
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = authorize(exam);

        var grade = schoolGradeRepository.findById(input.schoolGradeId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy khối"));
        var gradeLevel = schoolGradeLevelRepository.findById(grade.getSchoolGradeLevelId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy khối"));
        if (!gradeLevel.getSchoolId().equals(exam.getSchoolId())) {
            throw new ForbiddenException("Khối không thuộc trường của bài kiểm tra");
        }

        var now = OffsetDateTime.now();
        var classes = schoolClassRepository.findBySchoolId(
            exam.getSchoolId(), null, null, null, grade.getId(), 1, MAX_CLASSES).content();

        var activeUserIds = new LinkedHashSet<UUID>();
        for (var schoolClass : classes) {
            var roster = schoolClassUserRepository.findBySchoolClassId(
                schoolClass.getId(), 0, MAX_CLASS_ROSTER_SIZE).content();
            for (var classUser : roster) {
                if (classUser.isActive()) {
                    activeUserIds.add(classUser.getUserId());
                }
            }
        }

        var studentIds = userRoleQueryRepository.findUserIdsByRoleCode(activeUserIds, SchoolRoleCodes.STUDENT);
        var existingStudentIds = examCandidateRepository.findStudentIdsByExamId(exam.getId());

        var newCandidates = new ArrayList<ExamCandidate>();
        for (var userId : activeUserIds) {
            if (!studentIds.contains(userId)) {
                continue;
            }
            if (existingStudentIds.contains(userId)) {
                continue;
            }
            newCandidates.add(ExamCandidate.createFresh(exam.getId(), userId, currentUserId, now));
        }

        if (newCandidates.isEmpty()) {
            return List.of();
        }
        return ExamCandidateDtoMapper.toDtoList(examCandidateRepository.saveAll(newCandidates));
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
