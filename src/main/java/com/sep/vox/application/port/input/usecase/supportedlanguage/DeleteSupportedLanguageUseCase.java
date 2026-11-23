package com.sep.vox.application.port.input.usecase.supportedlanguage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.DeleteSupportedLanguageCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;

@Service
@RequiredArgsConstructor
public class DeleteSupportedLanguageUseCase implements IUseCase<DeleteSupportedLanguageCommand, Void> {

    private final SupportedLanguageRepository supportedLanguageRepository;

    @Override
    @Transactional
    public Void execute(DeleteSupportedLanguageCommand input) {
        SupportedLanguage language = supportedLanguageRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngôn ngữ"));

        language.setActive(false);
        supportedLanguageRepository.save(language);
        return null;
    }
}
