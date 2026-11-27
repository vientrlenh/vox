package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.AddSystemRubricVersionsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddSystemRubricVersionsUseCase implements IUseCase<AddSystemRubricVersionsCommand, List<UUID>> {

    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public List<UUID> execute(AddSystemRubricVersionsCommand command) {
        requireActiveCurrentUser();
        Rubric rubric = loadSystemOwnedRubric(command.rubricId());

        Set<Integer> existingVersions = rubricVersionRepository.findByRubricId(command.rubricId()).stream()
                .map(rv -> rv.getVersion())
                .collect(Collectors.toSet());

        Set<Integer> incomingVersions = new HashSet<>();
        Instant now = Instant.now();

        List<RubricVersion> newVersions = command.versions().stream()
                .map(vCmd -> toDraftVersion(command, rubric, vCmd, existingVersions, incomingVersions, now))
                .toList();

        List<RubricVersion> savedVersions = rubricVersionRepository.saveAll(newVersions);

        return savedVersions.stream().map(v -> v.getId()).toList();
    }

    private void requireActiveCurrentUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");
    }

    private Rubric loadSystemOwnedRubric(UUID rubricId) {
        Rubric rubric = rubricRepository.findById(rubricId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Bộ Rubric này thuộc trường học. System Admin không được phép thêm Version.");
        }
        return rubric;
    }

    private void requireNewVersionWithValidScale(AddSystemRubricVersionsCommand.RubricVersionItemCommand vCmd,
                                                 Set<Integer> existingVersions, Set<Integer> incomingVersions) {
        if (!incomingVersions.add(vCmd.version())) {
            throw new IllegalArgumentException("Hệ thống lỗi: Bạn đang gửi nhiều dữ liệu có cùng số Version (" + vCmd.version() + ").");
        }
        if (existingVersions.contains(vCmd.version())) {
            throw new IllegalArgumentException("Hệ thống lỗi: Version " + vCmd.version() + " đã tồn tại trong bộ Rubric này.");
        }
        if (vCmd.scoringScaleMin().compareTo(vCmd.scoringScaleMax()) > 0) {
            throw new IllegalArgumentException("Version " + vCmd.version() + ": Điểm sàn không được lớn hơn điểm trần.");
        }
    }

    private RubricVersion toDraftVersion(AddSystemRubricVersionsCommand command, Rubric rubric,
                                         AddSystemRubricVersionsCommand.RubricVersionItemCommand vCmd,
                                         Set<Integer> existingVersions, Set<Integer> incomingVersions, Instant now) {
        requireNewVersionWithValidScale(vCmd, existingVersions, incomingVersions);

        Instant validFrom = vCmd.effectiveFrom() != null ? vCmd.effectiveFrom() : now;
        if (vCmd.effectiveTo() != null && vCmd.effectiveTo().isBefore(validFrom)) {
            throw new IllegalArgumentException("Version " + vCmd.version() + ": Ngày kết thúc không được nằm trước ngày bắt đầu.");
        }

        String safeCode = rubric.getCode() + "_V" + vCmd.version();
        String safeName = (vCmd.name() != null && !vCmd.name().isBlank())
                ? vCmd.name().trim()
                : rubric.getName() + " - Version " + vCmd.version();

        return RubricVersion.draft(
                command.rubricId(), vCmd.version(), safeCode, safeName, rubric.getDescription(),
                validFrom, vCmd.effectiveTo(),
                vCmd.scoringScaleMin(), vCmd.scoringScaleMax(), vCmd.totalScoreMethod());
    }
}
