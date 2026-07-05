package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSystemRubricCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UpdateSystemRubricUseCase implements IUseCase<UpdateSystemRubricCommand, UUID> {

    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public UpdateSystemRubricUseCase(
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSystemRubricCommand command) {
        // 1. Xác thực tài khoản System Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản bị khóa.");
        }

        // 2. Lấy bộ Rubric gốc ra (Để kiểm tra sự tồn tại và Quyền)
        Rubric rubric = rubricRepository.findById(command.rubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric này."));

        // 3. Chốt chặn an ninh
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Rubric này thuộc về Trường học. System Admin không có quyền can thiệp.");
        }

        // 4. Chuẩn hóa dữ liệu đầu vào (Nếu có gửi lên thì mới trim)
        String safeName = (command.name() != null && !command.name().isBlank())
                ? StringNormalization.trimAndCollapseSpaces(command.name())
                : null;

        String safeDesc = (command.description() != null && !command.description().isBlank())
                ? StringNormalization.trimAndCollapseSpaces(command.description())
                : null;

        // 5. ATOMIC UPDATE BẰNG SQL THUẦN (Sử dụng COALESCE)
        rubricRepository.updateRubricAtomic(
                command.rubricId(),
                safeName,
                safeDesc
        );

        // 6. Trả về UUID
        return command.rubricId();
    }
}