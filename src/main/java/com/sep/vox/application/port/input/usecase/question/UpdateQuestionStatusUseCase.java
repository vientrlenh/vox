package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateQuestionStatusUseCase implements IUseCase<UpdateQuestionStatusCommand, QuestionDto> {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public UpdateQuestionStatusUseCase(
            QuestionRepository questionRepository,
            QuestionBankRepository questionBankRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public QuestionDto execute(UpdateQuestionStatusCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = !userContextPort.isSystemAdmin()
            && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
                .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        var question = questionRepository.findById(command.questionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        var bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        var owner = currentUserId.equals(question.getCreatedBy());
        var editorCollaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId)
            .filter(collaborator -> collaborator.getPermission() == QuestionCollaboratorPermission.CAN_EDIT)
            .isPresent();
        var systemAdminOnSystemBank = userContextPort.isSystemAdmin()
            && bank.getOwnerType() == QuestionBankOwnerType.SYSTEM;
        var schoolAdminOnSchoolBank = bank.getOwnerType() == QuestionBankOwnerType.SCHOOL
            && schoolAdmin
            && currentSchoolId != null
            && currentSchoolId.equals(bank.getSchoolId());

        switch (command.action()) {
            case "SUBMIT" -> {
                if (!owner && !editorCollaborator) {
                    throw new ForbiddenException("Quyền truy cập bị từ chối");
                }
                if (question.getStatus() != QuestionStatus.DRAFT
                        && question.getStatus() != QuestionStatus.REVISION_REQUESTED) {
                    throw new IllegalStateException("Chỉ được submit khi câu hỏi ở trạng thái DRAFT hoặc REVISION_REQUESTED");
                }
                question.setStatus(QuestionStatus.SUBMITTED_FOR_REVIEW);
            }
            case "APPROVE" -> {
                validateReviewPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank, editorCollaborator, owner);
                requireStatus(question.getStatus(), QuestionStatus.SUBMITTED_FOR_REVIEW);
                question.setStatus(QuestionStatus.APPROVED);
            }
            case "REJECT" -> {
                validateReviewPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank, editorCollaborator, owner);
                requireStatus(question.getStatus(), QuestionStatus.SUBMITTED_FOR_REVIEW);
                requireNote(command.note(), "REJECT");
                question.setStatus(QuestionStatus.REJECTED);
            }
            case "REQUEST_REVISION" -> {
                validateReviewPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank, editorCollaborator, owner);
                requireStatus(question.getStatus(), QuestionStatus.SUBMITTED_FOR_REVIEW);
                requireNote(command.note(), "REQUEST_REVISION");
                question.setStatus(QuestionStatus.REVISION_REQUESTED);
            }
            case "PUBLISH" -> {
                if (question.getStatus() == QuestionStatus.ARCHIVED) {
                    validateAdminPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank);
                } else {
                    requireStatus(question.getStatus(), QuestionStatus.APPROVED);
                    if (!owner && !editorCollaborator && !systemAdminOnSystemBank && !schoolAdminOnSchoolBank) {
                        throw new ForbiddenException("Quyền truy cập bị từ chối");
                    }
                }
                question.setStatus(QuestionStatus.PUBLISHED);
            }
            case "ARCHIVE" -> {
                validateAdminPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank);
                requireStatus(question.getStatus(), QuestionStatus.PUBLISHED);
                question.setStatus(QuestionStatus.ARCHIVED);
            }
            case "REOPEN" -> {
                validateAdminPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank);
                requireStatus(question.getStatus(), QuestionStatus.ARCHIVED);
                question.setStatus(QuestionStatus.DRAFT);
            }
            case "LOCK" -> {
                validateAdminPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank);
                if (question.isLocked()) {
                    throw new IllegalStateException("Câu hỏi đã bị khóa");
                }
                question.setLocked(true);
            }
            case "UNLOCK" -> {
                validateAdminPermission(systemAdminOnSystemBank, schoolAdminOnSchoolBank);
                if (!question.isLocked()) {
                    throw new IllegalStateException("Câu hỏi chưa bị khóa");
                }
                question.setLocked(false);
            }
            default -> throw new IllegalStateException("Action không hợp lệ");
        }

        question.setUpdatedAt(OffsetDateTime.now());
        question.setUpdatedBy(currentUserId);
        var saved = questionRepository.save(question);
        return QuestionDtoMapper.toQuestionDto(saved);
    }

    private void validateAdminPermission(boolean systemAdminOnSystemBank, boolean schoolAdminOnSchoolBank) {
        if (!systemAdminOnSystemBank && !schoolAdminOnSchoolBank) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }

    private void validateReviewPermission(
            boolean systemAdminOnSystemBank,
            boolean schoolAdminOnSchoolBank,
            boolean editorCollaborator,
            boolean owner) {
        if (systemAdminOnSystemBank || schoolAdminOnSchoolBank) {
            return;
        }
        if (editorCollaborator && !owner) {
            return;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }

    private void requireStatus(QuestionStatus actual, QuestionStatus expected) {
        if (actual != expected) {
            throw new IllegalStateException("Trạng thái câu hỏi hiện tại không hợp lệ cho action này");
        }
    }

    private void requireStatusIn(QuestionStatus actual, QuestionStatus... expected) {
        for (var status : expected) {
            if (actual == status) {
                return;
            }
        }
        throw new IllegalStateException("Trạng thái câu hỏi hiện tại không hợp lệ cho action này");
    }

    private void requireNote(String note, String action) {
        if (note == null || note.isBlank()) {
            throw new IllegalStateException("Action " + action + " bắt buộc phải có note");
        }
    }

    private UpdateQuestionStatusCommand normalize(UpdateQuestionStatusCommand input) {
        return new UpdateQuestionStatusCommand(
            input.questionId(),
            StringNormalization.normalizeCode(input.action()),
            StringNormalization.trimAndCollapseSpaces(input.note())
        );
    }
}
