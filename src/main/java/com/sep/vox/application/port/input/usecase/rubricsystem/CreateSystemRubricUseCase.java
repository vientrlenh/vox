package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSystemRubricCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.*;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;
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

    public CreateSystemRubricUseCase(
            RubricRepository rubricRepository,
            RubricVersionRepository rubricVersionRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SupportedLanguageRepository languageRepository) {
        this.rubricRepository = rubricRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.languageRepository = languageRepository;
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

        boolean isSystemRubricExisted = rubricRepository.existsByOwnerTypeAndLanguageId(RubricOwnerType.SYSTEM, command.languageId());
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
                RubricOwnerType.SYSTEM, // SYSTEM Owner
                null,                   // schoolId = null
                null
        );

        Rubric savedRubric = rubricRepository.save(newRubric);

        // 4. XỬ LÝ DANH SÁCH VERSION (THÊM BỘ LỌC CHỐNG TRÙNG LẶP)
        Set<Integer> uniqueVersions = new HashSet<>(); // Bổ sung dòng này

        List<RubricVersion> versionsToSave = command.versions().stream().map(vCmd -> {

            // Bổ sung Block Validate chống trùng version
            if (!uniqueVersions.add(vCmd.version())) {
                throw new DuplicatedException("Hệ thống lỗi: Bạn đang gửi nhiều phiên bản có cùng số Version (" + vCmd.version() + ").");
            }

            // Validate từng version
            if (vCmd.scoringScaleMin().compareTo(vCmd.scoringScaleMax()) > 0) {
                throw new IllegalArgumentException("Version " + vCmd.version() + ": Điểm sàn không được lớn hơn điểm trần.");
            }
            // Logic Effective From (Xử lý an toàn)
            OffsetDateTime validFrom = vCmd.effectiveFrom() != null ? vCmd.effectiveFrom() : now;
            if (vCmd.effectiveTo() != null && vCmd.effectiveTo().isBefore(validFrom)) {
                throw new IllegalArgumentException("Version " + vCmd.version() + ": Ngày kết thúc không được trước ngày bắt đầu.");
            }

            return new RubricVersion(
                    savedRubric.getId(),
                    vCmd.version(),
                    safeCode + "_V" + vCmd.version(),
                    safeName + " - Version " + vCmd.version(),
                    safeDesc,
                    RubricStatus.DRAFT, // Mặc định là DRAFT
                    validFrom, // Đã bọc an toàn
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
}