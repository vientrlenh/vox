package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.CreateSystemRubricCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateSystemRubricUseCase implements IUseCase<CreateSystemRubricCommand, UUID> {

    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SupportedLanguageRepository languageRepository;
    private final FrameworkRepository frameworkRepository;

    @Override
    @Transactional
    public UUID execute(CreateSystemRubricCommand command) {
        requireActiveCurrentUser();
        requireExistingLanguage(command.languageId());
        requireActiveFramework(command.frameworkId());

        String safeCode = StringNormalization.trimAndCollapseSpaces(command.code());
        String safeName = StringNormalization.trimAndCollapseSpaces(command.name());
        String safeDesc = command.description() != null ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;

        Rubric newRubric = Rubric.forSystem(
                command.languageId(), command.frameworkId(), safeCode, safeName, safeDesc);

        Rubric savedRubric = rubricRepository.save(newRubric);

        return savedRubric.getId();
    }

    private void requireActiveCurrentUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }
    }

    private void requireExistingLanguage(UUID languageId) {
        if (!languageRepository.existsById(languageId)) {
            throw new ResourceNotFoundException("Ngôn ngữ không tồn tại.");
        }
    }

    private void requireActiveFramework(UUID frameworkId) {
        Framework framework = frameworkRepository.findById(frameworkId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Khung tiêu chuẩn (Framework)."));
        if (!framework.isActive() ) {
            throw new IllegalArgumentException("Không thể sử dụng Khung tiêu chuẩn (Framework) này vì nó đang bị vô hiệu hóa.");
        }
    }
}
