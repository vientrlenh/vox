package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.AddSystemRubricVersionsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AddSystemRubricVersionsUseCase implements IUseCase<AddSystemRubricVersionsCommand, List<UUID>> {

    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public AddSystemRubricVersionsUseCase(
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
    public List<UUID> execute(AddSystemRubricVersionsCommand command) {
        // 1. Xác thực tài khoản Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        // 2. Lấy bộ Rubric gốc và kiểm tra quyền
        var rubric = rubricRepository.findById(command.rubricId()).orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Bộ Rubric này thuộc trường học. System Admin không được phép thêm Version.");
        }

        // 3. Lấy danh sách Version cũ để đối chiếu
        Set<Integer> existingVersions = rubricVersionRepository.findByRubricId(command.rubricId()).stream()
                .map(rv -> rv.getVersion())
                .collect(Collectors.toSet());

        Set<Integer> incomingVersions = new HashSet<>();
        Instant now = Instant.now();

        // 4. Map và Validate từng version
        List<RubricVersion> newVersions = command.versions().stream().map(vCmd -> {

            // Check trùng trong cùng 1 request
            if (!incomingVersions.add(vCmd.version())) {
                throw new IllegalArgumentException("Hệ thống lỗi: Bạn đang gửi nhiều dữ liệu có cùng số Version (" + vCmd.version() + ").");
            }
            // Check trùng với Database
            if (existingVersions.contains(vCmd.version())) {
                throw new IllegalArgumentException("Hệ thống lỗi: Version " + vCmd.version() + " đã tồn tại trong bộ Rubric này.");
            }
            // Check điểm sàn/trần
            if (vCmd.scoringScaleMin().compareTo(vCmd.scoringScaleMax()) > 0) {
                throw new IllegalArgumentException("Version " + vCmd.version() + ": Điểm sàn không được lớn hơn điểm trần.");
            }

            // Check enum Method
            RubricTotalScoreMethod method;
            try {
                method = RubricTotalScoreMethod.valueOf(vCmd.totalScoreMethod().trim().toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException("Version " + vCmd.version() + ": Phương pháp tính điểm không hợp lệ (Chỉ nhận SUM hoặc WEIGHTED_AVERAGE).");
            }

            // Check ngày tháng
            Instant validFrom = vCmd.effectiveFrom() != null ? vCmd.effectiveFrom() : now;
            if (vCmd.effectiveTo() != null && vCmd.effectiveTo().isBefore(validFrom)) {
                throw new IllegalArgumentException("Version " + vCmd.version() + ": Ngày kết thúc không được nằm trước ngày bắt đầu.");
            }

            String safeCode = rubric.getCode() + "_V" + vCmd.version();
            String safeName = (vCmd.name() != null && !vCmd.name().isBlank())
                    ? vCmd.name().trim()
                    : rubric.getName() + " - Version " + vCmd.version();

            return new RubricVersion(
                    command.rubricId(), vCmd.version(), safeCode, safeName, rubric.getDescription(),
                    RubricStatus.DRAFT, validFrom, vCmd.effectiveTo(), vCmd.scoringScaleMin(), vCmd.scoringScaleMax(),
                    method, now, now, currentUserId, currentUserId
            );
        }).toList();

        // 5. Lưu vào DB và trả về ID của các version vừa tạo
        List<RubricVersion> savedVersions = rubricVersionRepository.saveAll(newVersions);

        return savedVersions.stream().map(v -> v.getId()).toList();
    }
}
