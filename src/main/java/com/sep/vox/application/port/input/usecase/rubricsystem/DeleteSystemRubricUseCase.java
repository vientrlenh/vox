package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteSystemRubricCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DeleteSystemRubricUseCase implements IUseCase<DeleteSystemRubricCommand, Void> {

    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public DeleteSystemRubricUseCase(
            RubricRepository rubricRepository,
            RubricVersionRepository rubricVersionRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricRepository = rubricRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteSystemRubricCommand command) {
        // 1. Xác thực tài khoản Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        // 2. Lấy Rubric
        Rubric rubric = rubricRepository.findById(command.rubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric hệ thống này."));

        // 3. THIẾT QUÂN LUẬT: System Admin tuyệt đối không được xóa Rubric của Trường (School)
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Hành động bị từ chối: System Admin không thể can thiệp vào Rubric của các trường học.");
        }

        // 5. Quét các Version con xem có bản nào khác DRAFT không
        List<RubricVersion> versions = rubricVersionRepository.findByRubricId(rubric.getId());
        boolean hasLockedVersion = versions.stream()
                .anyMatch(v -> v.getStatus() != RubricStatus.DRAFT);

        if (hasLockedVersion) {
            throw new IllegalStateException("Không thể xóa Rubric này vì có phiên bản không phải là bản Nháp (DRAFT).");
        }

        // 6. Thực hiện Xóa cứng an toàn
        try {
            rubricRepository.deleteById(rubric.getId());
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Không thể xóa Rubric hệ thống này vì bên trong đang chứa dữ liệu Phiên bản. Hãy xóa các bản Nháp bên trong trước.");
        }

        return null;
    }
}