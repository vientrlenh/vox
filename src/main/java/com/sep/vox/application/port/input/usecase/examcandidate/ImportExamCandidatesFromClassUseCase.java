package com.sep.vox.application.port.input.usecase.examcandidate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ImportExamCandidatesFromClassCommand;
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

@Service
public class ImportExamCandidatesFromClassUseCase
        implements IUseCase<ImportExamCandidatesFromClassCommand, List<ExamCandidateDto>> {

    private static final int MAX_CLASS_ROSTER_SIZE = 1000;

    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final ExamDirectoryAccessService examDirectoryAccessService;

    public ImportExamCandidatesFromClassUseCase(
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            ExamDirectoryAccessService examDirectoryAccessService) {
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.examDirectoryAccessService = examDirectoryAccessService;
    }

    @Override
    @Transactional
    public List<ExamCandidateDto> execute(ImportExamCandidatesFromClassCommand input) {
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var scope = examDirectoryAccessService.resolve(exam);
        var currentUserId = scope.callerId();

        var schoolClass = schoolClassRepository.findById(input.schoolClassId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học"));
        if (!schoolClass.getSchoolId().equals(exam.getSchoolId())) {
            throw new ForbiddenException("Lớp học không thuộc trường của bài kiểm tra");
        }
        // Chủ tịch một bài trên lớp chỉ nhập được lớp mình phụ trách — khớp đúng phạm vi
        // mà `ViewExamDirectoryClassesUseCase` cho họ nhìn thấy.
        if (!scope.schoolWide()
                && !examDirectoryAccessService.callerClassIds(scope).contains(schoolClass.getId())) {
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
