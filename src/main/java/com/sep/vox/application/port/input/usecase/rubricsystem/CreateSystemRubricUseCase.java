package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSystemRubricCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CreateSystemRubricUseCase implements IUseCase<CreateSystemRubricCommand, UUID> {

    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SupportedLanguageRepository languageRepository;
    private final FrameworkRepository frameworkRepository;

    public CreateSystemRubricUseCase(
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SupportedLanguageRepository languageRepository,
            FrameworkRepository frameworkRepository) {
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.languageRepository = languageRepository;
        this.frameworkRepository = frameworkRepository;
    }

    @Override
    @Transactional
    public UUID execute(CreateSystemRubricCommand command) {
        // 1. Kiểm tra tài khoản và quyền SYSTEM_ADMIN
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // 2. Validate hệ thống
        if (!languageRepository.existsById(command.languageId())) {
            throw new NotFoundException("Ngôn ngữ không tồn tại.");
        }

        // Kiểm tra Framework gốc
        Framework framework = frameworkRepository.findById(command.frameworkId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Khung tiêu chuẩn (Framework)."));
        if (!framework.isActive() ) {
            throw new IllegalStateException("Không thể sử dụng Khung tiêu chuẩn (Framework) này vì nó đang bị vô hiệu hóa.");
        }



        // ĐÃ XÓA TOÀN BỘ LOGIC RÀNG BUỘC VÀ TẠO RUBRIC VERSION Ở ĐÂY

        String safeCode = StringNormalization.trimAndCollapseSpaces(command.code());
        String safeName = StringNormalization.trimAndCollapseSpaces(command.name());
        String safeDesc = command.description() != null ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;

        // 3. TẠO RUBRIC SYSTEM (Vỏ bọc)
        Rubric newRubric = new Rubric(
                command.languageId(),
                command.frameworkId(),
                safeCode,
                safeName,
                safeDesc,
                RubricOwnerType.SYSTEM,
                null
        );

        Rubric savedRubric = rubricRepository.save(newRubric);

        return savedRubric.getId(); // Trả về ID của vỏ Rubric vừa tạo
    }
}