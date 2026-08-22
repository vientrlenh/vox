package com.sep.vox.application.port.input.usecase.gradelevel;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Khối lớp là catalog TOÀN CỤC nên chỉ system admin được tạo -- trước đây use case này cho phép
 * school admin tạo khối riêng cho trường mình, nay một bản ghi ảnh hưởng mọi trường.
 */
@Service
public class CreateGradeLevelUseCase implements IUseCase<CreateGradeLevelCommand, UUID> {

    private final GradeLevelRepository gradeLevelRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public CreateGradeLevelUseCase(
            GradeLevelRepository gradeLevelRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.gradeLevelRepository = gradeLevelRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(CreateGradeLevelCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        validateSystemAdmin(currentUserId);

        String normalizedCode = StringNormalization.normalizeCode(command.code());
        validateUniqueness(normalizedCode, command.order());

        return saveNewGradeLevel(command, normalizedCode, currentUserId);
    }

    private void validateSystemAdmin(UUID userId) {
        if (!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Chỉ quản trị hệ thống mới được tạo khối học.");
        }
    }

    private void validateUniqueness(String code, Integer order) {
        if (gradeLevelRepository.existsByCode(code)) {
            throw new DuplicatedException("Mã khối học đã tồn tại.");
        }
        if (order != null && gradeLevelRepository.existsByOrder(order)) {
            throw new DuplicatedException("Thứ tự khối học đã được sử dụng.");
        }
    }

    private UUID saveNewGradeLevel(CreateGradeLevelCommand command, String code, UUID creatorId) {
        Instant now = Instant.now();
        GradeLevel newGradeLevel = new GradeLevel(
                code,
                StringNormalization.trimAndCollapseSpaces(command.name()),
                command.description() != null ? StringNormalization.trimAndCollapseSpaces(command.description()) : null,
                command.order(),
                GradeLevelStatus.ACTIVE,
                now, now, creatorId, creatorId
        );
        try {
            return gradeLevelRepository.save(newGradeLevel).getId();
        } catch (DataIntegrityViolationException e) {
            // Chống race-condition: hai request cùng tạo trùng mã/thứ tự vượt qua check exists rồi mới đụng unique index.
            throw new DuplicatedException("Mã hoặc thứ tự khối học đã tồn tại.");
        }
    }
}
