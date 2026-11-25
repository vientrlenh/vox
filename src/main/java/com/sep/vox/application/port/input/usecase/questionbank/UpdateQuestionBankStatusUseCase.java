package com.sep.vox.application.port.input.usecase.questionbank;

import com.sep.vox.domain.model.question.QuestionBank;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.questionbank.UpdateQuestionBankStatusCommand;
import com.sep.vox.application.port.input.service.QuestionBankManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.mapper.QuestionBankDtoMapper;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;

@Service
@RequiredArgsConstructor
public class UpdateQuestionBankStatusUseCase implements IUseCase<UpdateQuestionBankStatusCommand, QuestionBankDto> {

    private final QuestionBankRepository questionBankRepository;
    private final UserContextPort userContextPort;
    private final QuestionBankManageAccessService questionBankManageAccessService;

    @Override
    @Transactional
    public QuestionBankDto execute(UpdateQuestionBankStatusCommand input) {
        UpdateQuestionBankStatusCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        QuestionBank questionBank = questionBankRepository.findById(command.id())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        questionBankManageAccessService.requireManageAccess(questionBank, currentUserId);

        switch (command.action()) {
            case "PUBLISH" -> {
                if (questionBank.getStatus() != QuestionBankStatus.DRAFT) {
                    throw new IllegalArgumentException("Chỉ được publish ngân hàng câu hỏi đang ở trạng thái DRAFT");
                }
                questionBank.setStatus(QuestionBankStatus.PUBLISHED);
            }
            case "ARCHIVE" -> questionBank.setStatus(QuestionBankStatus.ARCHIVED);
            default -> throw new IllegalArgumentException("Action không hợp lệ");
        }


        QuestionBank saved = questionBankRepository.save(questionBank);
        return QuestionBankDtoMapper.toDto(saved);
    }

    private UpdateQuestionBankStatusCommand normalize(UpdateQuestionBankStatusCommand input) {
        return new UpdateQuestionBankStatusCommand(
            input.id(),
            StringNormalization.normalizeCode(input.action())
        );
    }
}
