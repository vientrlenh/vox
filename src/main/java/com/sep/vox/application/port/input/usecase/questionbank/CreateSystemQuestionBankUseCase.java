package com.sep.vox.application.port.input.usecase.questionbank;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.mapper.questionbank.CreateQuestionBankResponseMapper;
import com.sep.vox.application.port.input.command.questionbank.CreateSystemQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.questionbank.CreateQuestionBankResponse;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;

@Service
@RequiredArgsConstructor
public class CreateSystemQuestionBankUseCase implements IUseCase<CreateSystemQuestionBankCommand, CreateQuestionBankResponse> {

    private final QuestionBankRepository questionBankRepository;
    private final SupportedLanguageRepository supportedLanguageRepository;

    @Override
    @Transactional
    public CreateQuestionBankResponse execute(CreateSystemQuestionBankCommand input) {
        CreateSystemQuestionBankCommand command = normalize(input);

        if (!supportedLanguageRepository.existsByIdAndIsActive(command.languageId(), true)) {
            throw new IllegalArgumentException("The requested language does not exist or is not active");
        }

        QuestionBank questionBank = QuestionBank.create(
            command.languageId(), 
            null, 
            command.code(), 
            command.name(), 
            command.description(), 
            QuestionBankOwnerType.SYSTEM
        );

        QuestionBank saved = questionBankRepository.save(questionBank);
        return CreateQuestionBankResponseMapper.toResponse(saved.getId());
    }

    private CreateSystemQuestionBankCommand normalize(CreateSystemQuestionBankCommand input) {
        return new CreateSystemQuestionBankCommand(
            input.languageId(), 
            StringNormalization.normalizeCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
    }
}
