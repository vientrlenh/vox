package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.mapper.rubric.RubricCriterionDtoMapper;
import com.sep.vox.application.port.input.query.rubricschool.SearchSchoolRubricCriteriaQuery;
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
        requireActiveSchoolMember(query.schoolId());
        requireSchoolOwnedVersion(query);

        PageResult<RubricCriterion> pageResult = rubricCriterionRepository.searchRubricCriteria(query.versionId(), query.keyword(), query.isRequired(), query.page(), query.size()
        );
        return new PageResult<>(
                pageResult.content().stream()
                        .map(rubricCriterionDtoMapper::toDto)
                        .toList(),
                pageResult.page(), pageResult.size(), pageResult.totalElements(), pageResult.totalPages()
        );
    }

    private void requireActiveSchoolMember(UUID schoolId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account has been locked.");
        }

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("The account is not linked to any school."));
        if (!schoolUser.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("SECURITY: you are not allowed to interfere with the data of another school.");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found."));
        if (!school.isActive()) {
            throw new ForbiddenException("Action denied: this school is disabled.");
        }
    }

    private void requireSchoolOwnedVersion(SearchSchoolRubricCriteriaQuery query) {
        RubricVersion version = rubricVersionRepository.findById(query.versionId())
                .orElseThrow(() -> new ResourceNotFoundException("Rubric version not found."));
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Rubric not found."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || rubric.getSchoolId() == null || !rubric.getSchoolId().equals(query.schoolId())) {
            throw new ForbiddenException("SECURITY: this Rubric is not owned by your school.");
        }
    }
}