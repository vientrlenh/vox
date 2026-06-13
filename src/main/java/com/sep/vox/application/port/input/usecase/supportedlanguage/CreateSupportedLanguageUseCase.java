package com.sep.vox.application.port.input.usecase.supportedlanguage;

import java.time.OffsetDateTime;

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

        var saved = supportedLanguageRepository.save(language);
        return CreateSupportedLanguageResponseMapper.toResponse(saved.getId());
    }

    private CreateSupportedLanguageCommand normalize(CreateSupportedLanguageCommand input) {
        return new CreateSupportedLanguageCommand(
            StringNormalization.normalizeCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
    }
}
