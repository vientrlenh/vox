package com.sep.vox.application.port.input.usecase.supportedlanguage;

import java.time.OffsetDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.mapper.supportedlanguage.CreateSupportedLanguageResponseMapper;
import com.sep.vox.application.port.input.command.CreateSupportedLanguageCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.supportedlanguage.CreateSupportedLanguageResponse;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.LanguageCode;

@Service
public class CreateSupportedLanguageUseCase implements IUseCase<CreateSupportedLanguageCommand, CreateSupportedLanguageResponse> {

    private static final int MAX_CODE_LENGTH = 10;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 2048;

    private final SupportedLanguageRepository supportedLanguageRepository;
    private final UserContextPort userContextPort;

    public CreateSupportedLanguageUseCase(
            SupportedLanguageRepository supportedLanguageRepository,
            UserContextPort userContextPort) {
        this.supportedLanguageRepository = supportedLanguageRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public CreateSupportedLanguageResponse execute(CreateSupportedLanguageCommand input) {
        var command = normalize(input);
        validateCommand(command);

        if (supportedLanguageRepository.findByCode(command.code()).isPresent()) {
            throw new DuplicatedException("Ngôn ngữ đã tồn tại với mã: " + command.code());
        }

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var now = OffsetDateTime.now();
        var language = new SupportedLanguage(
            new LanguageCode(command.code()),
            command.name(),
            command.description(),
            true,
            now,
            now,
            currentUserId,
            currentUserId
        );

        try {
            var saved = supportedLanguageRepository.save(language);
            return CreateSupportedLanguageResponseMapper.toResponse(saved.getId());
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatedException("Ngôn ngữ đã tồn tại với mã: " + command.code());
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
            throw new IllegalArgumentException("Mã ngôn ngữ không được để trống");
        }
        if (code.length() > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException("Mã ngôn ngữ không được vượt quá 10 ký tự");
        }
        new LanguageCode(code);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên ngôn ngữ không được để trống");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Tên ngôn ngữ không được vượt quá 100 ký tự");
        }
    }

    private void validateDescription(String description) {
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Mô tả ngôn ngữ không được vượt quá 2048 ký tự");
        }
    }
}
