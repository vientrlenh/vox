package com.sep.vox.application.port.input.usecase.question;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteQuestionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.question.DeleteQuestionResponse;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class DeleteQuestionUseCase implements IUseCase<DeleteQuestionCommand, DeleteQuestionResponse> {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public DeleteQuestionUseCase(
            QuestionRepository questionRepository,
            QuestionBankRepository questionBankRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public DeleteQuestionResponse execute(DeleteQuestionCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = !userContextPort.isSystemAdmin()
            && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
                .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        var question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        var bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        var owner = currentUserId.equals(question.getCreatedBy());
        var systemAdminOnSystemBank = userContextPort.isSystemAdmin()
            && bank.getOwnerType() == QuestionBankOwnerType.SYSTEM;
        var schoolAdminOnSchoolBank = bank.getOwnerType() == QuestionBankOwnerType.SCHOOL
            && schoolAdmin
            && currentSchoolId != null
            && currentSchoolId.equals(bank.getSchoolId());
        var usedInExam = questionRepository.existsUsedInExam(question.getId());

        if (owner && question.getStatus() == QuestionStatus.DRAFT && !usedInExam) {
            questionRepository.deleteById(question.getId());
            return new DeleteQuestionResponse(true, false);
        }

        if (!systemAdminOnSystemBank && !schoolAdminOnSchoolBank) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (usedInExam) {
            question.setStatus(QuestionStatus.ARCHIVED);
            question.setUpdatedAt(Instant.now());
            question.setUpdatedBy(currentUserId);
            questionRepository.save(question);
            return new DeleteQuestionResponse(false, true);
        }

        questionRepository.deleteById(question.getId());
        return new DeleteQuestionResponse(true, false);
    }
}
