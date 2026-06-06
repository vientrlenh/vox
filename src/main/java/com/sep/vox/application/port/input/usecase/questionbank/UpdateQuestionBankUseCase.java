package com.sep.vox.application.port.input.usecase.questionbank;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.mapper.QuestionBankDtoMapper;
import com.sep.vox.domain.repository.QuestionBankRepository;

@Service
public class UpdateQuestionBankUseCase implements IUseCase<UpdateQuestionBankCommand, QuestionBankDto> {

    private final QuestionBankRepository questionBankRepository;
    private final UserContextPort userContextPort;

    public UpdateQuestionBankUseCase(QuestionBankRepository questionBankRepository, UserContextPort userContextPort) {
        this.questionBankRepository = questionBankRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public QuestionBankDto execute(UpdateQuestionBankCommand input) {
        var command = normalize(input);
        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var questionBank = questionBankRepository.findById(command.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        questionBank.setDescription(command.description());
        questionBank.setUpdatedAt(now);
        questionBank.setUpdatedBy(currentUserId);

        var saved = questionBankRepository.save(questionBank);
        return QuestionBankDtoMapper.toDto(saved);
    }

    private UpdateQuestionBankCommand normalize(UpdateQuestionBankCommand input) {
        return new UpdateQuestionBankCommand(
            input.id(),
            StringNormalization.trimAndCollapseSpaces(input.bankName()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
            input.isActive()
        );
    }
}
