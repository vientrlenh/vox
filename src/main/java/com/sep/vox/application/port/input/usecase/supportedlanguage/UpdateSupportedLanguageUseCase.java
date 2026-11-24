package com.sep.vox.application.port.input.usecase.supportedlanguage;


import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.supportedlanguage.UpdateSupportedLanguageCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.supportedlanguage.UpdateSupportedLanguageResponse;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.LanguageCode;

@Service
@RequiredArgsConstructor
public class UpdateSupportedLanguageUseCase implements IUseCase<UpdateSupportedLanguageCommand, UpdateSupportedLanguageResponse> {

    private static final int MAX_CODE_LENGTH = 10;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 2048;

    private final SupportedLanguageRepository supportedLanguageRepository;

    @Override
    @Transactional
    public UpdateSupportedLanguageResponse execute(UpdateSupportedLanguageCommand input) {
        UpdateSupportedLanguageCommand command = normalize(input);
        validateCommand(command);
        validateUniqueCode(command);

        try {
            int updatedRows = supportedLanguageRepository.updateMutableFields(
                command.id(),
                command.code(),
                command.codeProvided(),
                command.name(),
                command.nameProvided(),
                command.description(),
                command.descriptionProvided(),
                command.isActive(),
                command.isActiveProvided()
            );
            if (updatedRows == 0) {
                throw new ResourceNotFoundException("Không tìm thấy ngôn ngữ");
            }
            return new UpdateSupportedLanguageResponse(command.id());
        } catch (DataConflictException e) {
            throw new ResourceDuplicatedException("Ngôn ngữ đã tồn tại với mã: " + command.code());
        }
    }

    private UpdateSupportedLanguageCommand normalize(UpdateSupportedLanguageCommand input) {
        return new UpdateSupportedLanguageCommand(
            input.id(),
            input.codeProvided() ? StringNormalization.normalizeCode(input.code()) : null,
            input.codeProvided(),
            input.nameProvided() ? StringNormalization.trimAndCollapseSpaces(input.name()) : null,
            input.nameProvided(),
            input.descriptionProvided() && input.description() != null
                ? StringNormalization.trimAndCollapseSpaces(input.description())
                : null,
            input.descriptionProvided(),
            input.isActive(),
            input.isActiveProvided()
        );
    }

    private void validateCommand(UpdateSupportedLanguageCommand command) {
        if (!command.codeProvided() && !command.nameProvided() && !command.descriptionProvided() && !command.isActiveProvided()) {
            throw new IllegalArgumentException("Cần cung cấp ít nhất một trường để cập nhật");
        }
        if (command.codeProvided()) {
            validateCode(command.code());
        }
        if (command.nameProvided()) {
            validateName(command.name());
        }
        if (command.descriptionProvided() && command.description() != null) {
            validateDescription(command.description());
        }
        if (command.isActiveProvided() && command.isActive() == null) {
            throw new IllegalArgumentException("Trạng thái ngôn ngữ không hợp lệ");
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

    private void validateUniqueCode(UpdateSupportedLanguageCommand command) {
        if (!command.codeProvided()) {
            return;
        }
        supportedLanguageRepository.findByCode(command.code())
            .filter(existing -> !existing.getId().equals(command.id()))
            .ifPresent(existing -> {
                throw new ResourceDuplicatedException("Ngôn ngữ đã tồn tại với mã: " + command.code());
            });
    }
}
