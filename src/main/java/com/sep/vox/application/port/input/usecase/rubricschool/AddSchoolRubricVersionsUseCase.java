package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.AddSchoolRubricVersionsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AddSchoolRubricVersionsUseCase implements IUseCase<AddSchoolRubricVersionsCommand, UUID> {

    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public AddSchoolRubricVersionsUseCase(
            RubricRepository rubricRepository,
            RubricVersionRepository rubricVersionRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.rubricRepository = rubricRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(AddSchoolRubricVersionsCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        // 👉 CHECK QUYỀN TRƯỜNG HỌC
        var schoolUser = schoolUserRepository.findByUserId(currentUserId).orElseThrow(() -> new ForbiddenException("Không thuộc trường học nào."));
        if (!schoolUser.getSchoolId().equals(command.schoolId())) throw new ForbiddenException("BẢO MẬT: Bạn không có quyền thao tác trên trường này.");

        var rubric = rubricRepository.findById(command.rubricId()).orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || rubric.getSchoolId() == null || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Bảo mật: Bộ Rubric này không thuộc sở hữu của trường bạn.");
        }

        Set<Integer> existingVersions = rubricVersionRepository.findByRubricId(command.rubricId()).stream()
                .map(RubricVersion::getVersion)
                .collect(Collectors.toSet());

        Set<Integer> incomingVersions = new HashSet<>();
        OffsetDateTime now = OffsetDateTime.now();

        List<RubricVersion> newVersions = command.versions().stream().map(vCmd -> {
            if (!incomingVersions.add(vCmd.version())) {
                throw new IllegalArgumentException("Hệ thống lỗi: Bạn đang gửi nhiều dữ liệu có cùng số Version (" + vCmd.version() + ").");
            }
            if (existingVersions.contains(vCmd.version())) {
                throw new IllegalArgumentException("Hệ thống lỗi: Version " + vCmd.version() + " đã tồn tại trong bộ Rubric này.");
            }
            if (vCmd.scoringScaleMin().compareTo(vCmd.scoringScaleMax()) > 0) {
                throw new IllegalArgumentException("Version " + vCmd.version() + ": Điểm sàn không được lớn hơn điểm trần.");
            }

            RubricTotalScoreMethod method;
            try {
                method = RubricTotalScoreMethod.valueOf(vCmd.totalScoreMethod().trim().toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException("Version " + vCmd.version() + ": Phương pháp tính điểm không hợp lệ (Chỉ nhận SUM hoặc WEIGHTED_AVERAGE).");
            }

            OffsetDateTime validFrom = vCmd.effectiveFrom() != null ? vCmd.effectiveFrom() : now;
            if (vCmd.effectiveTo() != null && vCmd.effectiveTo().isBefore(validFrom)) {
                throw new IllegalArgumentException("Version " + vCmd.version() + ": Ngày kết thúc không được nằm trước ngày bắt đầu.");
            }

            String safeCode = rubric.getCode() + "_V" + vCmd.version();
            String safeName = rubric.getName() + " - Version " + vCmd.version();

            return new RubricVersion(
                    command.rubricId(), vCmd.version(), safeCode, safeName, rubric.getDescription(),
                    RubricStatus.DRAFT, validFrom, vCmd.effectiveTo(), vCmd.scoringScaleMin(), vCmd.scoringScaleMax(),
                    method, now, now, currentUserId, currentUserId
            );
        }).toList();

        rubricVersionRepository.saveAll(newVersions);

        return rubric.getId();
    }
}