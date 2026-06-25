package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSystemRubricCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.rubric.*;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CreateSystemRubricUseCase implements IUseCase<CreateSystemRubricCommand, UUID> {

    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SupportedLanguageRepository languageRepository;
    private final FrameworkRepository frameworkRepository;

    public CreateSystemRubricUseCase(
            RubricRepository rubricRepository,
            RubricVersionRepository rubricVersionRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SupportedLanguageRepository languageRepository,
            FrameworkRepository frameworkRepository) {
        this.rubricRepository = rubricRepository;
        this.rubricVersionRepository = rubricVersionRepository;
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
        if (!framework.isActive()) {
            throw new IllegalStateException("Không thể sử dụng Khung tiêu chuẩn (Framework) này vì nó đang bị vô hiệu hóa.");
        }

        boolean isSystemRubricExisted = rubricRepository.existsByOwnerTypeAndLanguageId(RubricOwnerType.SYSTEM.toString(), command.languageId());
        if (isSystemRubricExisted) {
            throw new IllegalStateException("Hệ thống đã có một bộ Rubric cho ngôn ngữ này. Chỉ được phép tồn tại duy nhất 1 bộ Rubric hệ thống cho mỗi ngôn ngữ.");
        }

        if (command.versions() == null || command.versions().isEmpty()) {
            throw new IllegalArgumentException("Rubric hệ thống phải có ít nhất 1 phiên bản.");
        }

        OffsetDateTime now = OffsetDateTime.now();
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

        // 4. XỬ LÝ DANH SÁCH VERSION
        Set<Integer> uniqueVersions = new HashSet<>();

        List<RubricVersion> versionsToSave = command.versions().stream().map(vCmd -> {

            // Block Validate chống trùng version
            if (!uniqueVersions.add(vCmd.version())) {
                throw new DuplicatedException("Hệ thống lỗi: Bạn đang gửi nhiều phiên bản có cùng số Version (" + vCmd.version() + ").");
            }

            // Validate từng version
            if (vCmd.scoringScaleMin().compareTo(vCmd.scoringScaleMax()) > 0) {
                throw new IllegalArgumentException("Version " + vCmd.version() + ": Điểm sàn không được lớn hơn điểm trần.");
            }

            // Logic Effective From (Xử lý an toàn)
            OffsetDateTime validFrom = getOffsetDateTime(vCmd, now);

            return new RubricVersion(
                    savedRubric.getId(),
                    vCmd.version(),
                    safeCode + "_V" + vCmd.version(),
                    safeName + " - Version " + vCmd.version(),
                    safeDesc,
                    RubricStatus.DRAFT, // Mặc định là DRAFT
                    validFrom,
                    vCmd.effectiveTo(),
                    vCmd.scoringScaleMin(),
                    vCmd.scoringScaleMax(),
                    vCmd.totalScoreMethod(),
                    now, now, currentUserId, currentUserId
            );
        }).toList();

        rubricVersionRepository.saveAll(versionsToSave);

        return savedRubric.getId();
    }

    private static @NonNull OffsetDateTime getOffsetDateTime(CreateSystemRubricCommand.RubricVersionItemCommand vCmd, OffsetDateTime now) {
        OffsetDateTime validFrom = vCmd.effectiveFrom() != null ? vCmd.effectiveFrom() : now;

        // CHỐT CHẶN: Chặn ngày trong quá khứ (so sánh phần Ngày của local, bỏ qua phần Giờ để không bị lỗi múi giờ)
        if (validFrom.toLocalDate().isBefore(now.toLocalDate())) {
            throw new IllegalArgumentException("Version " + vCmd.version() + ": Ngày bắt đầu áp dụng (Effective From) không được nằm trong quá khứ.");
        }

        // Validate Effective To
        if (vCmd.effectiveTo() != null && vCmd.effectiveTo().isBefore(validFrom)) {
            throw new IllegalArgumentException("Version " + vCmd.version() + ": Ngày kết thúc không được trước ngày bắt đầu.");
        }
        return validFrom;
    }
}