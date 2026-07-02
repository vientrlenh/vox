package com.sep.vox.application.port.input.usecase.questionbank;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.mapper.QuestionBankDtoMapper;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

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
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);

        var questionBank = questionBankRepository.findById(command.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        validateAccess(questionBank.getOwnerType(), questionBank.getSchoolId(), currentSchoolId);

        if (questionBank.getStatus() != QuestionBankStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được cập nhật ngân hàng câu hỏi đang ở trạng thái DRAFT");
        }

        if (command.name() != null) {
            questionBank.setName(command.name());
        }
        questionBank.setDescription(command.description());
        questionBank.setUpdatedAt(OffsetDateTime.now());
        questionBank.setUpdatedBy(currentUserId);

        var saved = questionBankRepository.save(questionBank);
        return QuestionBankDtoMapper.toDto(saved);
    }

    private void validateAccess(QuestionBankOwnerType ownerType, java.util.UUID schoolId, java.util.UUID currentSchoolId) {
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
