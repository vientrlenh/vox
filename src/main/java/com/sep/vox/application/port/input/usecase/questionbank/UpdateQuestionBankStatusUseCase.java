package com.sep.vox.application.port.input.usecase.questionbank;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionBankStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.mapper.QuestionBankDtoMapper;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateQuestionBankStatusUseCase implements IUseCase<UpdateQuestionBankStatusCommand, QuestionBankDto> {

    private final QuestionBankRepository questionBankRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    public UpdateQuestionBankStatusUseCase(QuestionBankRepository questionBankRepository, UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository) {
        this.questionBankRepository = questionBankRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public QuestionBankDto execute(UpdateQuestionBankStatusCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);

        var questionBank = questionBankRepository.findById(command.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        validateAccess(questionBank.getOwnerType(), questionBank.getSchoolId(), currentSchoolId);

        switch (command.action()) {
            case "PUBLISH" -> {
                if (questionBank.getStatus() != QuestionBankStatus.DRAFT) {
                    throw new IllegalStateException("Chỉ được publish ngân hàng câu hỏi đang ở trạng thái DRAFT");
                }
                questionBank.setStatus(QuestionBankStatus.PUBLISHED);
            }
            case "ARCHIVE" -> questionBank.setStatus(QuestionBankStatus.ARCHIVED);
            default -> throw new IllegalStateException("Action không hợp lệ");
        }

        questionBank.setUpdatedAt(OffsetDateTime.now());
        questionBank.setUpdatedBy(currentUserId);

        var saved = questionBankRepository.save(questionBank);
        return QuestionBankDtoMapper.toDto(saved);
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

    private UpdateQuestionBankStatusCommand normalize(UpdateQuestionBankStatusCommand input) {
        return new UpdateQuestionBankStatusCommand(
            input.id(),
            StringNormalization.normalizeCode(input.action())
        );
    }
}
