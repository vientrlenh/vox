package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSchoolRubricVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UpdateSchoolRubricVersionUseCase implements IUseCase<UpdateSchoolRubricVersionCommand, UUID> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    public UpdateSchoolRubricVersionUseCase(
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SchoolRepository schoolRepository,
            SchoolUserRepository schoolUserRepository) {
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.schoolRepository = schoolRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSchoolRubricVersionCommand command) {
        // 1. Xác thực User
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        // 2. Chốt chặn an ninh bằng SchoolUser
        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản chưa liên kết với trường học."));
        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Không có quyền sửa dữ liệu trường khác.");
        }

        var school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Trường học này đang bị vô hiệu hóa.");
        }

        // 3. Lấy Version và kiểm tra DRAFT
        var version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric Version."));
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể cập nhật khi phiên bản đang ở trạng thái DRAFT.");
        }

        // 4. Lấy Rubric gốc và kiểm tra quyền SCHOOL
        var rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Hành động bị từ chối: Phiên bản này không thuộc về trường học của bạn.");
        }

        // 5. Chuẩn hóa & Validate Data (Atomic Update Logic)
        String safeCode = (command.code() != null && !command.code().isBlank()) ? StringNormalization.trimAndCollapseSpaces(command.code()) : null;
        String safeName = (command.name() != null && !command.name().isBlank()) ? StringNormalization.trimAndCollapseSpaces(command.name()) : null;
        String safeDesc = (command.description() != null && !command.description().isBlank()) ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;

        OffsetDateTime finalEffectiveFrom = command.effectiveFrom() != null ? command.effectiveFrom() : version.getEffectiveFrom();
        OffsetDateTime finalEffectiveTo = command.effectiveTo() != null ? command.effectiveTo() : version.getEffectiveTo();

        if (finalEffectiveTo != null && finalEffectiveFrom.isAfter(finalEffectiveTo)) {
            throw new IllegalArgumentException("Ngày bắt đầu hiệu lực (EffectiveFrom) không được lớn hơn ngày kết thúc (EffectiveTo).");
        }

        BigDecimal finalScoreMin = command.scoringScaleMin() != null ? command.scoringScaleMin() : version.getScoringScaleMin();
        BigDecimal finalScoreMax = command.scoringScaleMax() != null ? command.scoringScaleMax() : version.getScoringScaleMax();

        if (finalScoreMin.compareTo(finalScoreMax) > 0) {
            throw new IllegalArgumentException("Điểm sàn (Min) không được lớn hơn điểm trần (Max).");
        }

        String safeMethod = null;
        if (command.totalScoreMethod() != null) {
            try {
                safeMethod = RubricTotalScoreMethod.valueOf(command.totalScoreMethod()).name();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Phương pháp tính tổng điểm không hợp lệ (Chỉ nhận WEIGHTED_AVERAGE hoặc SUM).");
            }
        }

        // 6. Bắn SQL Atomic (Có bắt lỗi trùng Code)
        try {
            rubricVersionRepository.updateRubricVersionAtomic(
                    command.versionId(),
                    safeCode,
                    safeName,
                    safeDesc,
                    command.effectiveFrom(),
                    command.effectiveTo(),
                    command.scoringScaleMin(),
                    command.scoringScaleMax(),
                    safeMethod,
                    OffsetDateTime.now(),
                    currentUserId
            );
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Mã phiên bản (Code) này đã tồn tại trong bộ Rubric. Vui lòng chọn mã khác.");
        }

        return command.versionId();
    }
}