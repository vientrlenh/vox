package com.sep.vox.application.port.input.usecase.questionbank;

import java.time.Instant;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.mapper.questionbank.CreateQuestionBankResponseMapper;
import com.sep.vox.application.port.input.command.CreateSystemQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.questionbank.CreateQuestionBankResponse;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;

@Service
public class CreateSystemQuestionBankUseCase implements IUseCase<CreateSystemQuestionBankCommand, CreateQuestionBankResponse> {

    private final QuestionBankRepository questionBankRepository;
    private final SupportedLanguageRepository supportedLanguageRepository;
    private final UserContextPort userContextPort;

    public CreateSystemQuestionBankUseCase(QuestionBankRepository questionBankRepository, 
            SupportedLanguageRepository supportedLanguageRepository, UserContextPort userContextPort) {
        this.questionBankRepository = questionBankRepository;
        this.supportedLanguageRepository = supportedLanguageRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public CreateQuestionBankResponse execute(CreateSystemQuestionBankCommand input) {
        CreateSystemQuestionBankCommand command = normalize(input);

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        if (!supportedLanguageRepository.existsByIdAndIsActive(command.languageId(), true)) {
            throw new IllegalStateException("Ngôn ngữ yêu cầu không tồn tại hoặc không hoạt động");
        }

        Instant now = Instant.now();
        QuestionBank questionBank = QuestionBank.create(
            command.languageId(), 
            null, 
            command.code(), 
            command.name(), 
            command.description(), 
            QuestionBankOwnerType.SYSTEM, 
            now, 
            currentUserId
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
