package com.sep.vox.application.port.input.usecase.exampaper;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamPaperStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamPaperDto;
import com.sep.vox.domain.mapper.ExamPaperDtoMapper;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamPaperStatusUseCase implements IUseCase<UpdateExamPaperStatusCommand, ExamPaperDto> {

    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public UpdateExamPaperStatusUseCase(
            ExamPaperRepository examPaperRepository,
            ExamPaperItemRepository examPaperItemRepository,
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examPaperRepository = examPaperRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamPaperDto execute(UpdateExamPaperStatusCommand input) {
        var command = new UpdateExamPaperStatusCommand(
            input.paperId(),
            StringNormalization.normalizeCode(input.action()),
            StringNormalization.trimAndCollapseSpaces(input.note())
        );
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var paper = examPaperRepository.findById(command.paperId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));
        var exam = examRepository.findById(paper.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        switch (command.action()) {
            case "SUBMIT" -> {
                requireRole(paper.getExamId(), currentUserId, ExamMemberRole.AUTHOR);
                if (examPaperItemRepository.existsUnassignedItemByPaperId(paper.getId())) {
                    throw new IllegalStateException("Đề thi còn ô câu hỏi chưa được gán, không thể nộp duyệt");
                }
                requireTransition(paper, ExamPaperStatus.DRAFT, ExamPaperStatus.IN_REVIEW);
            }
            case "APPROVE" -> {
                requireReviewerOrAdminOverride(paper, exam.getSchoolId(), currentUserId);
                requireTransition(paper, ExamPaperStatus.IN_REVIEW, ExamPaperStatus.APPROVED);
            }
            case "REQUEST_REVISION" -> {
                requireReviewerOrAdminOverride(paper, exam.getSchoolId(), currentUserId);
                if (command.note() == null || command.note().isBlank()) {
                    throw new IllegalStateException("Yêu cầu sửa lại bắt buộc phải có góp ý");
                }
                requireTransition(paper, ExamPaperStatus.IN_REVIEW, ExamPaperStatus.DRAFT);
            }
            case "LOCK" -> {
                requireChairOrAdminOverride(paper, exam.getSchoolId(), currentUserId);
                requireTransition(paper, ExamPaperStatus.APPROVED, ExamPaperStatus.LOCKED);
            }
            case "REOPEN" -> {
                requireChairOrAdminOverride(paper, exam.getSchoolId(), currentUserId);
                requireTransition(paper, ExamPaperStatus.LOCKED, ExamPaperStatus.DRAFT);
            }
            default -> throw new IllegalStateException("Action không hợp lệ");
        }

        paper.setUpdatedAt(OffsetDateTime.now());
        paper.setUpdatedBy(currentUserId);
        return ExamPaperDtoMapper.toDto(examPaperRepository.save(paper));
    }

    private void requireRole(UUID examId, UUID currentUserId, ExamMemberRole role) {
        if (!examMemberRepository.existsByExamIdAndUserIdAndRole(examId, currentUserId, role)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }

    private void requireNotAuthor(ExamPaper paper, UUID currentUserId) {
        if (currentUserId.equals(paper.getCreatedBy())) {
            throw new ForbiddenException("Không được tự duyệt đề thi do chính mình tạo");
        }
    }

    private void requireReviewerOrAdminOverride(ExamPaper paper, UUID examSchoolId, UUID currentUserId) {
        // CHAIR có toàn quyền của REVIEWER (approve/request-revision), ngoài quyền lock/reopen riêng của CHAIR.
        if (examMemberRepository.existsByExamIdAndUserIdAndRole(paper.getExamId(), currentUserId, ExamMemberRole.REVIEWER)
                || examMemberRepository.existsByExamIdAndUserIdAndRole(paper.getExamId(), currentUserId, ExamMemberRole.CHAIR)) {
            requireNotAuthor(paper, currentUserId);
            return;
        }
        if (isSchoolAdminOverrideAllowed(paper, examSchoolId, currentUserId, ExamMemberRole.REVIEWER)) {
            return;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }

    private void requireChairOrAdminOverride(ExamPaper paper, UUID examSchoolId, UUID currentUserId) {
        if (examMemberRepository.existsByExamIdAndUserIdAndRole(paper.getExamId(), currentUserId, ExamMemberRole.CHAIR)) {
            requireNotAuthor(paper, currentUserId);
            return;
        }
        if (isSchoolAdminOverrideAllowed(paper, examSchoolId, currentUserId, ExamMemberRole.CHAIR)) {
            return;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }

    private boolean isSchoolAdminOverrideAllowed(ExamPaper paper, UUID examSchoolId, UUID currentUserId, ExamMemberRole role) {
        if (currentUserId.equals(paper.getCreatedBy())) {
            return false;
        }
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        if (currentSchoolId == null || !currentSchoolId.equals(examSchoolId)) {
            return false;
        }
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(roleInfo -> "SCHOOL_ADMIN".equals(roleInfo.roleCode()));
        if (!schoolAdmin) {
            return false;
        }
        return !examMemberRepository.existsByExamIdAndRoleExcludingUserId(paper.getExamId(), role, paper.getCreatedBy());
    }

    private void requireTransition(ExamPaper paper, ExamPaperStatus from, ExamPaperStatus to) {
        if (paper.getStatus() != from) {
            throw new IllegalStateException("Trạng thái đề thi hiện tại không hợp lệ cho action này");
        }
        paper.setStatus(to);
    }
}
