package com.sep.vox.application.port.input.usecase.supportedlanguage;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteSupportedLanguageCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.SupportedLanguageRepository;

@Service
public class DeleteSupportedLanguageUseCase implements IUseCase<DeleteSupportedLanguageCommand, Void> {

    private final SupportedLanguageRepository supportedLanguageRepository;
    private final UserContextPort userContextPort;

    public DeleteSupportedLanguageUseCase(
            SupportedLanguageRepository supportedLanguageRepository,
            UserContextPort userContextPort) {
        this.supportedLanguageRepository = supportedLanguageRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteSupportedLanguageCommand input) {
        var language = supportedLanguageRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngôn ngữ"));

        language.setActive(false);
        language.setUpdatedAt(Instant.now());
        language.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
        supportedLanguageRepository.save(language);
        return null;
    }
}
