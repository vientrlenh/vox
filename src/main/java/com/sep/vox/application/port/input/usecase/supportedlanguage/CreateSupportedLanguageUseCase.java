package com.sep.vox.application.port.input.usecase.supportedlanguage;

import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.mapper.supportedlanguage.CreateSupportedLanguageResponseMapper;
import com.sep.vox.application.port.input.command.supportedlanguage.CreateSupportedLanguageCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.supportedlanguage.CreateSupportedLanguageResponse;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.LanguageCode;

@Service
@RequiredArgsConstructor
public class CreateSupportedLanguageUseCase implements IUseCase<CreateSupportedLanguageCommand, CreateSupportedLanguageResponse> {

    private static final int MAX_CODE_LENGTH = 10;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 2048;

    private final SupportedLanguageRepository supportedLanguageRepository;

    @Override
    @Transactional
    public CreateSupportedLanguageResponse execute(CreateSupportedLanguageCommand input) {
        CreateSupportedLanguageCommand command = normalize(input);
        validateCommand(command);

        if (supportedLanguageRepository.findByCode(command.code()).isPresent()) {
            throw new ResourceDuplicatedException("Language already exists with code: " + command.code());
        }

        SupportedLanguage language = SupportedLanguage.defined(
            new LanguageCode(command.code()), command.name(), command.description());

        try {
            SupportedLanguage saved = supportedLanguageRepository.save(language);
            return CreateSupportedLanguageResponseMapper.toResponse(saved.getId());
        } catch (DataConflictException e) {
            throw new ResourceDuplicatedException("Language already exists with code: " + command.code());
        }
    }

    private CreateSupportedLanguageCommand normalize(CreateSupportedLanguageCommand input) {
        return new CreateSupportedLanguageCommand(
            StringNormalization.normalizeCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
    }

    private void validateCommand(CreateSupportedLanguageCommand command) {
        validateCode(command.code());
        validateName(command.name());
        if (command.description() != null) {
            validateDescription(command.description());
        }
    }

    private void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Language code must not be empty");
        }
        if (code.length() > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException("Language code must not exceed 10 characters");
        }
        new LanguageCode(code);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Language name must not be empty");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Language name must not exceed 100 characters");
        }
    }

    private void validateDescription(String description) {
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Language description must not exceed 2048 characters");
        }
    }
}
