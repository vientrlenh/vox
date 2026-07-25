package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSchoolRubricCriterionBandCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UpdateSchoolRubricCriterionBandUseCase implements IUseCase<UpdateSchoolRubricCriterionBandCommand, UUID> {

    private final RubricCriterionBandRepository rubricCriterionBandRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    public UpdateSchoolRubricCriterionBandUseCase(
            RubricCriterionBandRepository rubricCriterionBandRepository,
            RubricCriterionRepository rubricCriterionRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SchoolRepository schoolRepository,
            SchoolUserRepository schoolUserRepository) {
        this.rubricCriterionBandRepository = rubricCriterionBandRepository;
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
    public UUID execute(UpdateSchoolRubricCriterionBandCommand command) {
        // 1. Xác thực User & School Firewall
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản chưa liên kết với trường học."));
        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền sửa dữ liệu của trường khác.");
        }

        var school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Trường học này đang bị vô hiệu hóa.");
        }

        // 2. Lấy Band
        var band = rubricCriterionBandRepository.findById(command.bandId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Dải điểm (Band) này."));

        // 3. Lấy Criterion
        var criterion = rubricCriterionRepository.findById(band.getCriterionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Tiêu chí chứa Dải điểm này."));

        // 4. Lấy Version và kiểm tra DRAFT
        var version = rubricVersionRepository.findById(criterion.getRubricVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric Version."));
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể sửa Dải điểm khi phiên bản Rubric đang ở trạng thái DRAFT.");
        }

        // 5. Lấy Rubric gốc và kiểm tra quyền SCHOOL
        var rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Hành động bị từ chối: Dải điểm này không thuộc về trường học của bạn.");
        }

        // 6. Chuẩn hóa & Validate Logic (Code không được phép sửa sau khi tạo, luôn giữ nguyên)

        // Check chéo minScore <= maxScore (giữa data gửi lên và data cũ trong DB)
        BigDecimal finalMinScore = command.scoreMin() != null ? command.scoreMin() : band.getScoreMin();
        BigDecimal finalMaxScore = command.scoreMax() != null ? command.scoreMax() : band.getScoreMax();

        if (finalMinScore.compareTo(finalMaxScore) > 0) {
            throw new IllegalArgumentException("Điểm sàn (scoreMin) không được lớn hơn điểm trần (scoreMax).");
        }

        // 7. Bắn SQL Atomic Update
        try {
            rubricCriterionBandRepository.updateBandAtomic(
                    command.bandId(),
                    null,
                    command.scoreMin(),
                    command.scoreMax(),
                    OffsetDateTime.now(),
                    currentUserId
            );
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Không thể cập nhật Dải điểm do dữ liệu bị trùng hoặc không hợp lệ.");
        }

        return command.bandId();
    }
}