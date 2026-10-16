package com.sep.vox.application.port.input.usecase.questionbank;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

import vn.payos.exception.UnauthorizedException;

@Service
public class UpdateQuestionBankUseCase implements IUseCase<UpdateQuestionBankCommand, QuestionBankDto> {

    private final QuestionBankRepository questionBankRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    public UpdateQuestionBankUseCase(QuestionBankRepository questionBankRepository, UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository) {
        this.questionBankRepository = questionBankRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public QuestionBankDto execute(UpdateQuestionBankCommand input) {
        UpdateQuestionBankCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);

        QuestionBank questionBank = questionBankRepository.findById(command.id())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        validateAccess(questionBank.getOwnerType(), questionBank.getSchoolId(), currentSchoolId);

        if (questionBank.getStatus() != QuestionBankStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được cập nhật ngân hàng câu hỏi đang ở trạng thái DRAFT");
        }

        if (command.name() != null) {
            questionBank.setName(command.name());
        }
        questionBank.setDescription(command.description());
        questionBank.setUpdatedAt(Instant.now());
        questionBank.setUpdatedBy(currentUserId);

        QuestionBank saved = questionBankRepository.save(questionBank);
        return QuestionBankDto.toDto(saved);
    }

    private void validateAccess(QuestionBankOwnerType ownerType, UUID schoolId, UUID currentSchoolId) {
        if (ownerType == QuestionBankOwnerType.SYSTEM) {
            if (!userContextPort.isSystemAdmin()) {
                throw new ForbiddenException("Quyền truy cập bị từ chối");
            }
            return;
        }
        if (currentSchoolId == null || !currentSchoolId.equals(schoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }

    private UpdateQuestionBankCommand normalize(UpdateQuestionBankCommand input) {
        return new UpdateQuestionBankCommand(
            input.id(),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
    }
}
