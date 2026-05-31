package com.sep.vox.application.port.input.usecase.questionbank;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.CreateQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.mapper.QuestionBankDtoMapper;
import com.sep.vox.domain.model.questionbank.QuestionBank;
import com.sep.vox.domain.repository.QuestionBankRepository;

@Service
public class CreateQuestionBankUseCase implements IUseCase<CreateQuestionBankCommand, QuestionBankDto> {

    private final QuestionBankRepository questionBankRepository;
    private final UserContextPort userContextPort;

    public CreateQuestionBankUseCase(QuestionBankRepository questionBankRepository, UserContextPort userContextPort) {
        this.questionBankRepository = questionBankRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public QuestionBankDto execute(CreateQuestionBankCommand input) {
        var command = normalize(input);
        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var questionBank = new QuestionBank(
            command.bankName(),
            command.description(),
            true,
            now,
            now,
            currentUserId,
            currentUserId
        );

        var saved = questionBankRepository.save(questionBank);
        return QuestionBankDtoMapper.toDto(saved);
    }

    private CreateQuestionBankCommand normalize(CreateQuestionBankCommand input) {
        return new CreateQuestionBankCommand(
            StringNormalization.trimAndCollapseSpaces(input.bankName()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
    }
}
