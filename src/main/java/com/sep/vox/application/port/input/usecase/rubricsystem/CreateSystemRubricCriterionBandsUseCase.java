package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSystemRubricCriterionBandsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.*;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
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
public class CreateSystemRubricCriterionBandsUseCase implements IUseCase<CreateSystemRubricCriterionBandsCommand, List<UUID>> {

    private final RubricCriterionBandRepository rubricCriterionBandRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public CreateSystemRubricCriterionBandsUseCase(
            RubricCriterionBandRepository rubricCriterionBandRepository,
            RubricCriterionRepository rubricCriterionRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricCriterionBandRepository = rubricCriterionBandRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public List<UUID> execute(CreateSystemRubricCriterionBandsCommand command) {
        // 1. Xác thực tài khoản
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        // 2. Validate Version
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric."));

        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được thiết lập cấu hình điểm khi Rubric ở trạng thái DRAFT.");
        }

        // 3. Validate Rubric & Ownership (SYSTEM)
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Rubric này thuộc về Trường học. System Admin không có quyền can thiệp.");
        }

        // 4. Validate Criterion (Tiêu chí)
        RubricCriterion criterion = rubricCriterionRepository.findById(command.criterionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Tiêu chí này."));

        if (!criterion.getRubricVersionId().equals(version.getId())) {
            throw new IllegalArgumentException("Tiêu chí không thuộc về phiên bản Rubric đang chọn.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        Set<String> uniqueCodes = new HashSet<>();

        // 5. Lặp và xử lý các Band
        List<RubricCriterionBand> bandsToSave = command.bands().stream().map(bCmd -> {

            String safeCode = StringNormalization.trimAndCollapseSpaces(bCmd.code());

            // 5.1 Check trùng lặp Code nội bộ
            if (!uniqueCodes.add(safeCode)) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp Mã đánh giá (Code): " + safeCode);
            }

            // 5.2 Validate Min <= Max của chính cái Band
            if (bCmd.scoreMin().compareTo(bCmd.scoreMax()) > 0) {
                throw new IllegalArgumentException("Mức đánh giá '" + safeCode + "': Điểm tối thiểu không được lớn hơn điểm tối đa.");
            }

            // 5.3 Validate LOGIC NGHIỆP VỤ LÕI: Khoảng điểm của Band phải nằm gọn trong khoảng điểm của Tiêu chí
            if (bCmd.scoreMin().compareTo(criterion.getMinScore()) < 0 || bCmd.scoreMax().compareTo(criterion.getMaxScore()) > 0) {
                throw new IllegalArgumentException(
                        "Mức đánh giá '" + safeCode + "' có khoảng điểm [" + bCmd.scoreMin() + " - " + bCmd.scoreMax() +
                                "] vượt quá giới hạn điểm cho phép của tiêu chí [" + criterion.getMinScore() + " - " + criterion.getMaxScore() + "]."
                );
            }

            return new RubricCriterionBand(
                    command.criterionId(),
                    safeCode,
                    bCmd.scoreMin(),
                    bCmd.scoreMax(),
                    now, now, currentUserId, currentUserId
            );
        }).collect(Collectors.toList());

        // 6. Lưu hàng loạt xuống Database
        try {
            // Chú ý: Cần có hàm saveAll trong RubricCriterionBandRepository
            rubricCriterionBandRepository.saveAll(bandsToSave);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Lỗi lưu dữ liệu: Mã mức đánh giá (Code) đã được cấu hình cho tiêu chí này từ trước.");
        }

        return bandsToSave.stream().map(RubricCriterionBand::getId).collect(Collectors.toList());
    }
}