package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.mapper.rubric.RubricCriterionDtoMapper;
import com.sep.vox.application.port.input.query.SearchSchoolRubricCriteriaQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricCriterionDto;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchSchoolRubricCriteriaUseCase implements IUseCase<SearchSchoolRubricCriteriaQuery, PageResult<RubricCriterionDto>> {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolRepository schoolRepository;
    private final RubricCriterionDtoMapper rubricCriterionDtoMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<RubricCriterionDto> execute(SearchSchoolRubricCriteriaQuery query) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // Kiểm tra liên kết trường của User
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản không được liên kết với trường học nào."));
        if (!schoolUser.getSchoolId().equals(query.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền can thiệp dữ liệu trường khác.");
        }

        // Kiểm tra trường học active
        School school = schoolRepository.findById(query.schoolId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa.");
        }

        // Kiểm tra liên kết Rubric
        RubricVersion version = rubricVersionRepository.findById(query.versionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản Rubric."));
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric gốc."));

        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || rubric.getSchoolId() == null || !rubric.getSchoolId().equals(query.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bộ tiêu chí này không thuộc quyền sở hữu của trường bạn.");
        }

        // Execute
        PageResult<RubricCriterion> pageResult = rubricCriterionRepository.searchRubricCriteria(query.versionId(), query.keyword(), query.isRequired(), query.page(), query.size()
        );

        return new PageResult<>(
                pageResult.content().stream()
                        .map(rubricCriterionDtoMapper::toDto)
                        .toList(),
                pageResult.page(), pageResult.size(), pageResult.totalElements(), pageResult.totalPages()
        );
    }
}