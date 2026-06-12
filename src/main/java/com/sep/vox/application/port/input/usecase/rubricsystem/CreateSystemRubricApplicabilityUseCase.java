package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSystemRubricApplicabilityCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.*;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CreateSystemRubricApplicabilityUseCase implements IUseCase<CreateSystemRubricApplicabilityCommand, List<UUID>> {

    private final RubricApplicabilityRepository rubricApplicabilityRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public CreateSystemRubricApplicabilityUseCase(RubricApplicabilityRepository rubricApplicabilityRepository, RubricVersionRepository rubricVersionRepository, RubricRepository rubricRepository, UserRepository userRepository, UserContextPort userContextPort) {
        this.rubricApplicabilityRepository = rubricApplicabilityRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public List<UUID> execute(CreateSystemRubricApplicabilityCommand command) {
        // 1. Kiểm tra System Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Lỗi tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        // 2. Version phải là PUBLISHED
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric."));


//        if (version.getStatus() != RubricStatus.PUBLISHED) {
//            throw new IllegalStateException("Hành động bị từ chối: Chỉ được thiết lập áp dụng cho các phiên bản đã được Ban hành (PUBLISHED).");
//        }

        // 3. Khóa OwnerType: Phải là SYSTEM
        Rubric rubric = rubricRepository.findById(version.getRubricId()).orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Hành động bị từ chối: Không thể cấu hình áp dụng hệ thống cho một Rubric thuộc về Trường học.");
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 4. Xử lý Logic thời gian (Gán classId và gradeId = null)
        List<RubricApplicability> applicabilitiesToSave = command.applicabilities().stream().map(appCmd -> {

            OffsetDateTime validFrom = appCmd.effectiveFrom() != null ? appCmd.effectiveFrom() : now;
            if (appCmd.effectiveTo() != null && appCmd.effectiveTo().isBefore(validFrom)) {
                throw new IllegalArgumentException("Ngày kết thúc áp dụng không được trước ngày bắt đầu.");
            }

            return new RubricApplicability(
                    command.versionId(),
                    null,
                    null,
                    validFrom,
                    appCmd.effectiveTo(),
                    now, now, currentUserId, currentUserId
            );
        }).toList();

        // 5. Lưu Database
        rubricApplicabilityRepository.saveAll(applicabilitiesToSave);

        return applicabilitiesToSave.stream().map(RubricApplicability::getId).toList();
    }
}