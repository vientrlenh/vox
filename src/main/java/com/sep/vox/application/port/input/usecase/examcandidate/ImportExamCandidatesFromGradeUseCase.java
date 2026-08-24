package com.sep.vox.application.port.input.usecase.examcandidate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ImportExamCandidatesFromGradeCommand;
import com.sep.vox.application.port.input.service.ExamDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamCandidateDto;
import com.sep.vox.domain.mapper.ExamCandidateDtoMapper;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.service.exam.ExamEditingGuard;

@Service
public class ImportExamCandidatesFromGradeUseCase
        implements IUseCase<ImportExamCandidatesFromGradeCommand, List<ExamCandidateDto>> {

    private static final int MAX_CLASSES = 200;
    private static final int MAX_CLASS_ROSTER_SIZE = 1000;

    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final ExamDirectoryAccessService examDirectoryAccessService;

    public ImportExamCandidatesFromGradeUseCase(
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            SchoolGradeRepository schoolGradeRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            ExamDirectoryAccessService examDirectoryAccessService) {
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.examDirectoryAccessService = examDirectoryAccessService;
    }

    @Override
    @Transactional
    public List<ExamCandidateDto> execute(ImportExamCandidatesFromGradeCommand input) {
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var scope = examDirectoryAccessService.resolve(exam);
        // Nhập theo niên khóa là gom mọi lớp của niên khóa đó — vượt xa phạm vi của chủ
        // tịch một bài trên lớp. Chặn ở đây để quyền ghi khớp đúng quyền đọc
        // (`ViewExamDirectoryGradesUseCase` từ chối cùng trường hợp này).
        if (!scope.schoolWide()) {
            throw new ForbiddenException("Bài kiểm tra trên lớp không hỗ trợ nhập thí sinh theo niên khóa");
        }
        var currentUserId = scope.callerId();
        ExamEditingGuard.requireScheduleEditable(exam);

        var grade = schoolGradeRepository.findById(input.schoolGradeId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy khối"));
        if (!grade.getSchoolId().equals(exam.getSchoolId())) {
            throw new ForbiddenException("Khối không thuộc trường của bài kiểm tra");
        }

        var now = Instant.now();
        var classes = schoolClassRepository.findBySchoolId(
            exam.getSchoolId(), null, null, null, grade.getId(), 1, MAX_CLASSES).content();

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
}
