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
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        // 1. Validate User & Permission
        validateUserAccess(currentUserId, command.schoolId());

        // 2. Validate Ngôn ngữ & Framework
        validateInfrastructure(command.languageId(), command.frameworkId());

        // 3. Kiểm tra Rubric duy nhất (Business Rule)
        validateRubricUniqueness(command.schoolId(), command.languageId());

        // 4. Lưu Rubric gốc
        Rubric savedRubric = saveRubric(command);

        // 5. Xử lý và lưu danh sách phiên bản
        saveRubricVersions(savedRubric.getId(), command, currentUserId);

        return savedRubric.getId();
    }

    private void validateUserAccess(UUID userId, UUID schoolId) {
        if (!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }

        // Tối ưu: Chỉ lấy schoolId thay vì toàn bộ thực thể
        schoolUserRepository.findSchoolIdByUserId(userId)
                .filter(id -> id.equals(schoolId))
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền quản lý trường học này."));

        // Kiểm tra trường active
        if (!schoolRepository.existsByIdAndIsActiveTrue(schoolId)) {
            throw new ForbiddenException("Trường học đang bị vô hiệu hóa.");
        }
    }

    private void validateInfrastructure(UUID langId, UUID frameworkId) {
        if (!languageRepository.existsById(langId)) throw new NotFoundException("Ngôn ngữ không tồn tại.");

        frameworkRepository.findById(frameworkId)
                .filter(Framework::isActive)
                .orElseThrow(() -> new NotFoundException("Framework không tồn tại hoặc bị vô hiệu hóa."));
    }

    private void validateRubricUniqueness(UUID schoolId, UUID langId) {
        if (rubricRepository.existsByOwnerTypeAndSchoolIdAndLanguageId(RubricOwnerType.SCHOOL.toString(), schoolId, langId)) {
            throw new IllegalStateException("Trường học đã có Rubric cho ngôn ngữ này.");
        }
    }

    private Rubric saveRubric(CreateSchoolRubricCommand command) {
        return rubricRepository.save(new Rubric(
                command.languageId(),
                command.frameworkId(),
                StringNormalization.trimAndCollapseSpaces(command.code()),
                StringNormalization.trimAndCollapseSpaces(command.name()),
                StringNormalization.trimAndCollapseSpaces(command.description()),
                RubricOwnerType.SCHOOL,
                command.schoolId()

        ));
    }

    private void saveRubricVersions(UUID rubricId, CreateSchoolRubricCommand command, UUID userId) {
        List<CreateSchoolRubricCommand.RubricVersionItemCommand> versionCmds = command.versions();
        if (versionCmds == null || versionCmds.isEmpty()) throw new IllegalArgumentException("Rubric cần ít nhất 1 phiên bản.");

        OffsetDateTime now = OffsetDateTime.now();
        Set<Integer> seenVersions = new HashSet<>();

        List<RubricVersion> versions = versionCmds.stream().map(vCmd -> {
            // Validation trong Stream
            if (!seenVersions.add(vCmd.version())) throw new IllegalArgumentException("Trùng phiên bản: " + vCmd.version());
            if (vCmd.scoringScaleMin().compareTo(vCmd.scoringScaleMax()) > 0) throw new IllegalArgumentException("Điểm sàn > trần.");

            OffsetDateTime validFrom = vCmd.effectiveFrom() != null ? vCmd.effectiveFrom() : now;
            if (vCmd.effectiveTo() != null && vCmd.effectiveTo().isBefore(validFrom)) throw new IllegalArgumentException("Ngày kết thúc không hợp lệ.");

            return new RubricVersion(
                    rubricId, vCmd.version(),
                    command.code() + "_V" + vCmd.version(),
                    command.name() + " - V" + vCmd.version(),
                    command.description(), RubricStatus.DRAFT,
                    validFrom, vCmd.effectiveTo(),
                    vCmd.scoringScaleMin(), vCmd.scoringScaleMax(),
                    vCmd.totalScoreMethod(), now, now, userId, userId
            );
        }).toList();

        rubricVersionRepository.saveAll(versions);
    }
}