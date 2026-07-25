package com.sep.vox.application.port.input.usecase.rubricschool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.vox.application.common.ScoreRangeValidator;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSchoolRubricCriterionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExample;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UpdateSchoolRubricCriterionUseCase implements IUseCase<UpdateSchoolRubricCriterionCommand, UUID> {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    // SỬA LỖI 1: Tự khởi tạo ObjectMapper ở đây để dùng cho việc dịch JSON
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UpdateSchoolRubricCriterionUseCase(
            RubricCriterionRepository rubricCriterionRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SchoolRepository schoolRepository,
            SchoolUserRepository schoolUserRepository) {
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
    public UUID execute(UpdateSchoolRubricCriterionCommand command) {
        // 1. Xác thực User
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        // 2. Chốt chặn an ninh bằng SchoolUser
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

        // 3. Lấy Criterion
        var criterion = rubricCriterionRepository.findById(command.criterionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Tiêu chí (Criterion) này."));

        // 4. Lấy Version và kiểm tra DRAFT
        var version = rubricVersionRepository.findById(criterion.getRubricVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric Version chứa tiêu chí này."));
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể sửa tiêu chí khi phiên bản Rubric đang ở trạng thái DRAFT.");
        }

        // 5. Lấy Rubric gốc và kiểm tra quyền SCHOOL (Đảm bảo Tiêu chí nằm trong bộ Rubric của trường)
        var rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Hành động bị từ chối: Tiêu chí này không thuộc về trường học của bạn.");
        }

        // 6. Chuẩn hóa & Validate Data (Code không được phép sửa sau khi tạo, luôn giữ nguyên)
        String safeName = (command.name() != null && !command.name().isBlank()) ? StringNormalization.trimAndCollapseSpaces(command.name()) : null;
        String safeDesc = (command.description() != null && !command.description().isBlank()) ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;

        if (command.weight() != null && command.weight().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Trọng số (weight) không được là số âm.");
        }
        if (command.order() != null && command.order() <= 0) {
            throw new IllegalArgumentException("Thứ tự (order) phải lớn hơn 0.");
        }

        BigDecimal finalMinScore = command.minScore() != null ? command.minScore() : criterion.getMinScore();
        BigDecimal finalMaxScore = command.maxScore() != null ? command.maxScore() : criterion.getMaxScore();

        if (finalMinScore.compareTo(finalMaxScore) > 0) {
            throw new IllegalArgumentException("Điểm tối thiểu (minScore) không được lớn hơn điểm tối đa (maxScore).");
        }

        // Validate nằm trong thang điểm tổng của RubricVersion
        String nameForError = safeName != null ? safeName : criterion.getName();
        ScoreRangeValidator.assertWithinScale(version.getScoringScaleMin(), version.getScoringScaleMax(),
                finalMinScore, finalMaxScore, nameForError);

        // 7. Validate chuỗi JSON (Bẫy bằng Value Object RubricCriterionExamples) & chuẩn hóa lại đúng định dạng {"values": [...]} trước khi lưu DB
        String examplesJsonToPersist = command.examplesJson();
        if (command.examplesJson() != null && !command.examplesJson().isBlank()) {
            try {
                // SỬA LỖI 2: Dùng objectMapper để parse JSON thành List object
                List<RubricCriterionExample> parsedExamples = objectMapper.readValue(
                        command.examplesJson(),
                        new TypeReference<List<RubricCriterionExample>>() {}
                );

                RubricCriterionExamples examplesVO = new RubricCriterionExamples(parsedExamples); // Kích hoạt ném lỗi nếu List rỗng hoặc chứa null

                // Serialize lại đúng shape record ({"values": [...]}) để khớp với định dạng khi đọc (view)
                examplesJsonToPersist = objectMapper.writeValueAsString(examplesVO);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e.getMessage());
            } catch (Exception e) {
                throw new IllegalArgumentException("Định dạng JSON của phần Examples không hợp lệ.");
            }
        }

        // 8. Bắn SQL Atomic Update
        try {
            rubricCriterionRepository.updateCriterionAtomic(
                    command.criterionId(),
                    null,
                    safeName,
                    safeDesc,
                    examplesJsonToPersist,
                    command.weight(),
                    command.minScore(),
                    command.maxScore(),
                    command.order(),
                    command.isRequired(),
                    OffsetDateTime.now(),
                    currentUserId
            );
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Không thể cập nhật Tiêu chí do dữ liệu bị trùng hoặc không hợp lệ.");
        }

        return command.criterionId();
    }
}