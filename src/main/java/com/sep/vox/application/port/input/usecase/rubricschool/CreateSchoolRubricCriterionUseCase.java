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
import com.sep.vox.domain.repository.*;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExample;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;
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
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository; // BỔ SUNG

    public CreateSchoolRubricCriterionUseCase(
            RubricCriterionRepository rubricCriterionRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort, SchoolRepository schoolRepository, SchoolUserRepository schoolUserRepository) {
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.schoolRepository = schoolRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public List<UUID> execute(CreateSchoolRubricCriteriaCommand command) {
        // 1. Xác thực tài khoản
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        // 2. Lấy Version và kiểm tra DRAFT
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric này."));

        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được phép thêm tiêu chí khi Rubric đang ở trạng thái Nháp (DRAFT).");
        }

        // 3. Kiểm tra Rubric gốc & Quyền
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));

        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được liên kết với bất kỳ trường học nào."));

        if (!rubric.getSchoolId().equals(command.schoolId()) || !schoolUser.getSchoolId().equals(rubric.getSchoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền chỉnh sửa Rubric của trường khác.");
        }

        // (Bổ sung) Kiểm tra xem trường học có đang hoạt động không
        var school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));

        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa trên hệ thống.");
        }



        OffsetDateTime now = OffsetDateTime.now();
        Set<String> uniqueCodes = new HashSet<>();
        Set<UUID> uniqueFrameworkIds = new HashSet<>();

        // 4. Xử lý danh sách tiêu chí
        List<RubricCriterion> criteriaToSave = command.criteria().stream().map(cCmd -> {

            String safeCode = StringNormalization.trimAndCollapseSpaces(cCmd.code());
            String safeName = StringNormalization.trimAndCollapseSpaces(cCmd.name());

            if (!uniqueCodes.add(safeCode)) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp Mã tiêu chí (Code): " + safeCode);
            }

            if (!uniqueFrameworkIds.add(cCmd.frameworkCriterionId())) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp Framework Criterion cho tiêu chí: " + safeName);
            }

            if (cCmd.minScore().compareTo(cCmd.maxScore()) > 0) {
                throw new IllegalArgumentException("Tiêu chí '" + safeCode + "': Điểm sàn không được lớn hơn điểm trần.");
            }

            // TẠO VALUE OBJECT CHO EXAMPLES (Đã fix lỗi .values())
            RubricCriterionExamples examplesVO = null;
            if (cCmd.examples() != null && !cCmd.examples().isEmpty()) {
                List<RubricCriterionExample> exampleList = cCmd.examples().stream()
                        .map(exCmd -> new RubricCriterionExample(exCmd.transcript(), exCmd.explanation(), exCmd.expectedScore()))
                        .toList();
                examplesVO = new RubricCriterionExamples(exampleList);
            }

            return new RubricCriterion(
                    command.versionId(),
                    cCmd.frameworkCriterionId(),
                    safeCode,
                    safeName,
                    cCmd.description() != null ? StringNormalization.trimAndCollapseSpaces(cCmd.description()) : null,
                    examplesVO, // Gắn Value Object vào đây
                    cCmd.weight(),
                    cCmd.minScore(),
                    cCmd.maxScore(),
                    cCmd.order(),
                    cCmd.isRequired(),
                    now, now, currentUserId, currentUserId
            );
        }).collect(Collectors.toList());

        // 5. Lưu xuống DB
        try {
            rubricCriterionRepository.saveAll(criteriaToSave);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Lỗi lưu dữ liệu: Mã tiêu chí hoặc Khung tiêu chuẩn đã tồn tại trong phiên bản này từ trước.");
        }

        return criteriaToSave.stream().map(RubricCriterion::getId).collect(Collectors.toList());
    }
}