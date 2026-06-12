package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSchoolRubricCriteriaCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CreateSchoolRubricCriterionUseCase implements IUseCase<CreateSchoolRubricCriteriaCommand, List<UUID>> {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public CreateSchoolRubricCriterionUseCase(
            RubricCriterionRepository rubricCriterionRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public List<UUID> execute(CreateSchoolRubricCriteriaCommand command) {

        // 1. Xác thực tài khoản (Bổ sung check ACTIVE)
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        // 2. Lấy Version và kiểm tra trạng thái DRAFT
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric này."));

        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được phép thêm tiêu chí khi Rubric đang ở trạng thái Nháp (DRAFT).");
        }

        // 3. Kiểm tra Rubric gốc & Quyền của Trường
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));

        if (!rubric.getSchoolId().equals(command.schoolId()) ||
                (currentUser.getSchoolId() != null && !currentUser.getSchoolId().equals(rubric.getSchoolId()))) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền chỉnh sửa Rubric của trường khác.");
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 4. Lặp và xử lý danh sách tiêu chí (CÓ CHỐNG TRÙNG LẶP)
        Set<String> uniqueCodes = new HashSet<>();
        Set<UUID> uniqueFrameworkIds = new HashSet<>();

        List<RubricCriterion> criteriaToSave = command.criteria().stream().map(cCmd -> {

            String safeCode = StringNormalization.trimAndCollapseSpaces(cCmd.code());
            String safeName = StringNormalization.trimAndCollapseSpaces(cCmd.name());

            // Check trùng mã Code ngay trong mảng gửi lên
            if (!uniqueCodes.add(safeCode)) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp Mã tiêu chí (Code): " + safeCode);
            }

            // Check trùng Framework Criterion ID ngay trong mảng gửi lên
            if (!uniqueFrameworkIds.add(cCmd.frameworkCriterionId())) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp Framework Criterion cho tiêu chí: " + safeName);
            }

            // Validate Logic điểm
            if (cCmd.minScore().compareTo(cCmd.maxScore()) > 0) {
                throw new IllegalArgumentException("Tiêu chí '" + safeCode + "': Điểm sàn không được lớn hơn điểm trần.");
            }

            return new RubricCriterion(
                    command.versionId(),
                    cCmd.frameworkCriterionId(),
                    safeCode,
                    safeName,
                    cCmd.description() != null ? StringNormalization.trimAndCollapseSpaces(cCmd.description()) : null,
                    cCmd.weight(),
                    cCmd.minScore(),
                    cCmd.maxScore(),
                    cCmd.order(),
                    cCmd.isRequired(),
                    now, now, currentUserId, currentUserId
            );
        }).collect(Collectors.toList());

        // 5. Lưu vào Database & Bắt lỗi trùng lặp với dữ liệu cũ
        try {
            rubricCriterionRepository.saveAll(criteriaToSave);
        } catch (DataIntegrityViolationException e) {
            // Lỗi này xảy ra khi safeCode hoặc frameworkCriterionId đã có sẵn trong bảng của Version này từ trước.
            throw new IllegalStateException("Lỗi lưu dữ liệu: Mã tiêu chí hoặc Khung tiêu chuẩn (Framework) đã tồn tại trong phiên bản Rubric này từ trước. Vui lòng kiểm tra lại.");
        }

        return criteriaToSave.stream().map(RubricCriterion::getId).collect(Collectors.toList());
    }
}