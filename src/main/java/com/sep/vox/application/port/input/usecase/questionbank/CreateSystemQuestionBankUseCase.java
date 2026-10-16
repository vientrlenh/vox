package com.sep.vox.application.port.input.usecase.questionbank;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSystemQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.questionbank.CreateQuestionBankResponse;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.language.LearningLanguageRepository;

@Service
public class CreateSystemQuestionBankUseCase implements IUseCase<CreateSystemQuestionBankCommand, CreateQuestionBankResponse> {

    private final QuestionBankRepository questionBankRepository;
    private final LearningLanguageRepository learningLanguageRepository;
    private final UserContextPort userContextPort;

    public CreateSystemQuestionBankUseCase(
            QuestionBankRepository questionBankRepository, 
            LearningLanguageRepository learningLanguageRepository,
            UserContextPort userContextPort
        ) {
        this.questionBankRepository = questionBankRepository;
        this.learningLanguageRepository = learningLanguageRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public CreateQuestionBankResponse execute(CreateSystemQuestionBankCommand input) {
        CreateSystemQuestionBankCommand command = normalize(input);

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));

        if (!learningLanguageRepository.existsByIdAndIsAvailable(command.learningLanguageId())) {
            throw new ResourceNotFoundException("Learning language is not found or is not working");
        }

        var now = Instant.now();
        var questionBank = QuestionBank.create(
            command.languageId(), 
            null, 
            command.code(), 
            command.name(), 
            command.description(), 
            QuestionBankOwnerType.SYSTEM, 
            now, 
            currentUserId
        );

        var saved = questionBankRepository.save(questionBank);
        return CreateQuestionBankResponse.toResponse(saved.getId());
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
