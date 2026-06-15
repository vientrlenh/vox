package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSchoolRubricCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
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
    private final FrameworkRepository frameworkRepository; // BỔ SUNG
    private final SchoolUserRepository schoolUserRepository;

    public CreateSchoolRubricUseCase(
            RubricRepository rubricRepository,
            RubricVersionRepository rubricVersionRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SupportedLanguageRepository languageRepository,
            FrameworkRepository frameworkRepository, SchoolUserRepository schoolUserRepository) { // BỔ SUNG
        this.rubricRepository = rubricRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.languageRepository = languageRepository;
        this.frameworkRepository = frameworkRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public UUID execute(CreateSchoolRubricCommand command) {

        // 1. Kiểm tra tài khoản & Quyền
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản đã bị khóa.");


        // 2. Kiểm tra Trường học (Active)
        School school = schoolRepository.findById(command.schoolId()).orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));

        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa trên hệ thống.");
        }

        // 3. KIỂM TRA QUYỀN SCHOOL BẰNG BẢNG SCHOOL_USER
        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được liên kết với bất kỳ trường học nào."));

        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Bạn không có quyền tạo Rubric cho trường khác.");
        }


        // 2. Kiểm tra Ngôn ngữ
        if (!languageRepository.existsById(command.languageId())) {
            throw new NotFoundException("Ngôn ngữ không tồn tại.");
        }

        // 3. BỔ SUNG: Kiểm tra Framework gốc
        Framework framework = frameworkRepository.findById(command.frameworkId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Khung tiêu chuẩn (Framework)."));
        if (!framework.isActive()) {
            throw new IllegalStateException("Không thể sử dụng Khung tiêu chuẩn (Framework) này vì nó đang bị vô hiệu hóa.");
        }

        // 4. Kiểm tra Rubric duy nhất theo ngôn ngữ
        boolean isRubricExisted = rubricRepository.existsByOwnerTypeAndSchoolIdAndLanguageId(
                RubricOwnerType.SCHOOL.toString(),
                command.schoolId(),
                command.languageId()
        );

        if (isRubricExisted) {
            throw new IllegalStateException("Trường học này đã khởi tạo một bộ Rubric cho ngôn ngữ được chọn. Mỗi trường chỉ được phép có tối đa 1 bộ Rubric cho mỗi ngôn ngữ.");
        }

        if (command.versions() == null || command.versions().isEmpty()) {
            throw new IllegalArgumentException("Rubric phải có ít nhất 1 phiên bản.");
        }

        // 5. TẠO RUBRIC GỐC (Map đúng chuẩn Constructor)
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
                null // currentVersionId ban đầu luôn null
        );

        Rubric savedRubric = rubricRepository.save(newRubric);

        // 6. XỬ LÝ DANH SÁCH VERSION
        OffsetDateTime now = OffsetDateTime.now();
        Set<Integer> uniqueVersions = new HashSet<>();

        List<RubricVersion> versionsToSave = command.versions().stream().map(vCmd -> {

            if (!uniqueVersions.add(vCmd.version())) {
                throw new IllegalArgumentException("Lỗi: Bạn đang gửi nhiều phiên bản có cùng số Version (" + vCmd.version() + ").");
            }

            if (vCmd.scoringScaleMin().compareTo(vCmd.scoringScaleMax()) > 0) {
                throw new IllegalArgumentException("Version " + vCmd.version() + ": Điểm sàn không được lớn hơn điểm trần.");
            }

            OffsetDateTime validFrom = vCmd.effectiveFrom() != null ? vCmd.effectiveFrom() : now;
            if (vCmd.effectiveTo() != null && vCmd.effectiveTo().isBefore(validFrom)) {
                throw new IllegalArgumentException("Version " + vCmd.version() + ": Ngày kết thúc không được trước ngày bắt đầu.");
            }

            // Map đúng chuẩn Constructor của Domain Model
            return new RubricVersion(
                    savedRubric.getId(),
                    vCmd.version(),
                    safeCode + "_V" + vCmd.version(),
                    safeName + " - Version " + vCmd.version(),
                    safeDesc,
                    RubricStatus.DRAFT, // Mặc định là Nháp
                    validFrom,
                    vCmd.effectiveTo(),
                    vCmd.scoringScaleMin(),
                    vCmd.scoringScaleMax(),
                    vCmd.totalScoreMethod(),
                    now, now, currentUserId, currentUserId
            );
        }).toList();

        // 7. Lưu hàng loạt
        rubricVersionRepository.saveAll(versionsToSave);

        return savedRubric.getId();
    }
}