package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.mapper.rubric.RubricCriterionDtoMapper;
import com.sep.vox.application.port.input.query.rubricschool.ViewSchoolRubricCriterionDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.RubricCriterionDto;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ViewSchoolRubricCriterionDetailsUseCase implements IUseCase<ViewSchoolRubricCriterionDetailsQuery, RubricCriterionDto> {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final RubricCriterionDtoMapper rubricCriterionDtoMapper;

    @Override
    @Transactional(readOnly = true)
    public RubricCriterionDto execute(ViewSchoolRubricCriterionDetailsQuery query) {
        requireActiveSchoolMember(query.schoolId());

        RubricCriterion criterion = rubricCriterionRepository.findById(query.criterionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Tiêu chí (Criterion) này."));
        requireSchoolOwnedCriterion(criterion, query.schoolId());

        return rubricCriterionDtoMapper.toDto(criterion);
    }

    private void requireActiveSchoolMember(UUID schoolId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản chưa liên kết với trường học."));
        if (!schoolUser.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền truy cập dữ liệu của trường khác.");
        }
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Trường học này đang bị vô hiệu hóa.");
        }
    }

    private void requireSchoolOwnedCriterion(RubricCriterion criterion, UUID schoolId) {
        RubricVersion version = rubricVersionRepository.findById(criterion.getRubricVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Rubric Version chứa tiêu chí này."));
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("Hành động bị từ chối: Tiêu chí này không thuộc về trường học của bạn.");
        }
    }
}