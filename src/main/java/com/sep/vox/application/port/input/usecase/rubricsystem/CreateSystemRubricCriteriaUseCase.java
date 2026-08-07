package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.domain.service.rubric.RubricOrderValidator;
import com.sep.vox.domain.service.rubric.ScoreRangeValidator;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSystemRubricCriteriaCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.FrameworkCriterion; // BỔ SUNG
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.FrameworkCriterionRepository; // BỔ SUNG
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExample;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CreateSystemRubricCriteriaUseCase implements IUseCase<CreateSystemRubricCriteriaCommand, List<UUID>> {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final FrameworkCriterionRepository frameworkCriterionRepository; // THÊM REPO NÀY

    public CreateSystemRubricCriteriaUseCase(
            RubricCriterionRepository rubricCriterionRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            FrameworkCriterionRepository frameworkCriterionRepository) {
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
    }

    @Override
    @Transactional
    public List<UUID> execute(CreateSystemRubricCriteriaCommand command) {

        // 1. Xác thực tài khoản
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        // 2. Validate Version
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric."));

        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được thêm tiêu chí khi Rubric ở trạng thái DRAFT.");
        }

        // 3. BẢO MẬT: Đảm bảo Rubric này thực sự là của SYSTEM
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Rubric này thuộc về một trường học. System Admin không được phép chỉnh sửa.");
        }

        // BƯỚC TỐI ƯU N+1: GOM ID VÀ QUERY 1 LẦN DUY NHẤT ĐỂ VALIDATE FRAMEWORK

        Set<UUID> inputFrameworkIds = command.criteria().stream()
                .map(c -> c.frameworkCriterionId())
                .collect(Collectors.toSet());

        List<FrameworkCriterion> existingFwCriteria = frameworkCriterionRepository.findAllByIds(inputFrameworkIds.stream().toList());

        // Nếu số lượng tìm thấy dưới DB không bằng số lượng Set ID gửi lên -> Có ID Fake!
        if (existingFwCriteria.size() != inputFrameworkIds.size()) {
            throw new NotFoundException("Dữ liệu không hợp lệ: Một hoặc nhiều Framework Criterion ID gửi lên không tồn tại trong hệ thống.");
        }


        Instant now = Instant.now();

        // 4. Lặp và xử lý danh sách tiêu chí (CÓ CHỐNG TRÙNG LẶP NỘI BỘ)
        Set<String> uniqueCodes = new HashSet<>();
        Set<UUID> uniqueFrameworkIds = new HashSet<>();

        // Tích luỹ order đã có trong version (DB) + order mới trong cùng batch để chống trùng thứ tự
        Set<Integer> ordersSoFar = new HashSet<>();
        rubricCriterionRepository.findByRubricVersionId(command.versionId())
                .forEach(c -> ordersSoFar.add(c.getOrder()));

        List<RubricCriterion> criteriaToSave = command.criteria().stream().map(cCmd -> {

            String safeCode = StringNormalization.trimAndCollapseSpaces(cCmd.code());
            String safeName = StringNormalization.trimAndCollapseSpaces(cCmd.name());

            // Check trùng mã Code trong RAM
            if (!uniqueCodes.add(safeCode)) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp Mã tiêu chí (Code): " + safeCode);
            }

            // Check trùng Framework Criterion ID trong RAM
            if (!uniqueFrameworkIds.add(cCmd.frameworkCriterionId())) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp Framework Criterion cho tiêu chí: " + safeName);
            }

            // Validate Logic nghiệp vụ
            if (cCmd.minScore().compareTo(cCmd.maxScore()) > 0) {
                throw new IllegalArgumentException("Tiêu chí '" + safeCode + "': Điểm sàn không được lớn hơn điểm trần.");
            }
            if (cCmd.weight() != null && cCmd.weight().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Tiêu chí '" + safeCode + "': Trọng số (weight) không được là số âm.");
            }

            // Check trùng thứ tự (order) với sibling đã có trong version hoặc trong cùng batch
            RubricOrderValidator.assertNoDuplicateOrder(ordersSoFar, cCmd.order(), safeName);

            // Validate nằm trong thang điểm tổng của RubricVersion
            ScoreRangeValidator.assertWithinScale(version.getScoringScaleMin(), version.getScoringScaleMax(),
                    cCmd.minScore(), cCmd.maxScore(), safeName);

            // XỬ LÝ VALUE OBJECT: Chuyển đổi dữ liệu example (Nếu có)
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
                    examplesVO,
                    cCmd.weight(),
                    cCmd.minScore(),
                    cCmd.maxScore(),
                    cCmd.order(),
                    cCmd.isRequired(),
                    now, now, currentUserId, currentUserId
            );
        }).collect(Collectors.toList());

        // 5. Lưu toàn bộ xuống SQL Server
        try {
            rubricCriterionRepository.saveAll(criteriaToSave);
        } catch (DataIntegrityViolationException e) {
            // Nhờ bước Validate DB phía trên, lúc này ta chắc chắn 100% DataIntegrityViolationException ở đây là lỗi Unique Constraint (Trùng lặp Code/Framework với dữ liệu CŨ đã lưu từ đợt trước)
            throw new IllegalStateException("Lỗi lưu dữ liệu: Mã tiêu chí, Khung tiêu chuẩn (Framework) hoặc Thứ tự (order) đã tồn tại trong phiên bản Rubric hệ thống này từ trước.");
        }

        return criteriaToSave.stream().map(rc -> rc.getId()).collect(Collectors.toList());
    }
}