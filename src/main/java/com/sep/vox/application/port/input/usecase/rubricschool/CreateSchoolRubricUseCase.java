package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSchoolRubricCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@Service
public class CreateSchoolRubricUseCase implements IUseCase<CreateSchoolRubricCommand, UUID> {

    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SupportedLanguageRepository languageRepository;


    public CreateSchoolRubricUseCase(
            RubricRepository rubricRepository,
            RubricVersionRepository rubricVersionRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            UserContextPort userContextPort, SupportedLanguageRepository languageRepository) {
        this.rubricRepository = rubricRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.languageRepository = languageRepository;

    }

    @Override
    @Transactional
    public UUID execute(CreateSchoolRubricCommand command) {

        // 1. Kiểm tra tài khoản & Quyền (Giữ nguyên)
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản đã bị khóa.");

        School school = schoolRepository.findById(command.schoolId()).orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));
        if (currentUser.getSchoolId() != null && !currentUser.getSchoolId().equals(school.getId())) {
            throw new ForbiddenException("Bạn không có quyền tạo Rubric cho trường khác.");
        }

        if (!languageRepository.existsById(command.languageId())) {
            throw new NotFoundException("Ngôn ngữ không tồn tại.");
        }

        boolean isRubricExisted = rubricRepository.existsByOwnerTypeAndSchoolIdAndLanguageId(
                RubricOwnerType.SCHOOL,
                command.schoolId(),
                command.languageId()
        );

        if (isRubricExisted) {
            throw new IllegalStateException("Trường học này đã khởi tạo một bộ Rubric cho ngôn ngữ được chọn. Mỗi trường chỉ được phép có tối đa 1 bộ Rubric cho mỗi ngôn ngữ (kể cả bản Nháp).");
        }

        if (command.versions() == null || command.versions().isEmpty()) {
            throw new IllegalArgumentException("Rubric phải có ít nhất 1 phiên bản.");
        }

        // 2. TẠO RUBRIC GỐC
        String safeCode = StringNormalization.trimAndCollapseSpaces(command.code());
        String safeName = StringNormalization.trimAndCollapseSpaces(command.name());
        String safeDesc = command.description() != null ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;

        Rubric newRubric = new Rubric(
                command.languageId(),
                command.frameworkId(),
                safeCode,
                safeName,
                safeDesc,
                RubricOwnerType.SCHOOL,
                command.schoolId(),
                null
        );

        Rubric savedRubric = rubricRepository.save(newRubric);

        // 3. XỬ LÝ DANH SÁCH VERSION (TỐI ƯU & CHỐNG LỖI)
        OffsetDateTime now = OffsetDateTime.now();
        Set<Integer> uniqueVersions = new HashSet<>(); // Dùng Set để phát hiện trùng lặp O(1)

        List<RubricVersion> versionsToSave = command.versions().stream().map(vCmd -> {

            // Validate 1: Chống gửi trùng version (VD: 2 cái đều là version 1)
            if (!uniqueVersions.add(vCmd.version())) {
                throw new IllegalArgumentException("Lỗi: Bạn đang gửi nhiều phiên bản có cùng số Version (" + vCmd.version() + ").");
            }

            // Validate 2: Điểm min max
            if (vCmd.scoringScaleMin().compareTo(vCmd.scoringScaleMax()) > 0) {
                throw new IllegalArgumentException("Version " + vCmd.version() + ": Điểm sàn không được lớn hơn điểm trần.");
            }

            // Validate 3: Logic thời gian (Bọc lót chống null vì DTO có thể bị bypass)
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
                    RubricStatus.DRAFT,
                    validFrom,
                    vCmd.effectiveTo(),
                    vCmd.scoringScaleMin(),
                    vCmd.scoringScaleMax(),
                    vCmd.totalScoreMethod(),
                    now, now, currentUserId, currentUserId
            );
        }).toList();

        // 4. Lưu Bulk/Batch xuống DB
        rubricVersionRepository.saveAll(versionsToSave);

        return savedRubric.getId();
    }
}