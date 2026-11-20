package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolRubricCriterionDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.RubricCriterionDto;
import com.sep.vox.domain.mapper.RubricCriterionDtoMapper;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ViewSchoolRubricCriterionDetailsUseCase implements IUseCase<ViewSchoolRubricCriterionDetailsQuery, RubricCriterionDto> {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    public ViewSchoolRubricCriterionDetailsUseCase(
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
    @Transactional(readOnly = true)
    public RubricCriterionDto execute(ViewSchoolRubricCriterionDetailsQuery query) {
        // 1. Xác thực User
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        // 2. Chốt chặn an ninh SchoolUser
        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản chưa liên kết với trường học."));
        if (!schoolUser.getSchoolId().equals(query.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền truy cập dữ liệu của trường khác.");
        }

        var school = schoolRepository.findById(query.schoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Trường học này đang bị vô hiệu hóa.");
        }

        // 3. Lấy Criterion ra
        var criterion = rubricCriterionRepository.findById(query.criterionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Tiêu chí (Criterion) này."));

        // 4. Lấy Version
        var version = rubricVersionRepository.findById(criterion.getRubricVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric Version chứa tiêu chí này."));

        // 5. Lấy Rubric gốc và kiểm tra quyền SCHOOL
        var rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));

        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(query.schoolId())) {
            throw new ForbiddenException("Hành động bị từ chối: Tiêu chí này không thuộc về trường học của bạn.");
        }

        // 6. Map sang DTO và trả về
        return RubricCriterionDtoMapper.toDto(criterion);
    }
}