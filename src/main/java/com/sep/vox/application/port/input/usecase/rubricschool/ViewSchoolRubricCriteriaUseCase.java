package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.mapper.rubric.RubricCriterionDtoMapper;
import com.sep.vox.application.port.input.query.rubricschool.ViewSchoolRubricCriteriaQuery;
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
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewSchoolRubricCriteriaUseCase implements IUseCase<ViewSchoolRubricCriteriaQuery, PageResult<RubricCriterionDto>> {

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
    public PageResult<RubricCriterionDto> execute(ViewSchoolRubricCriteriaQuery query) {
        requireActiveSchoolMember(query.schoolId());
        requireSchoolOwnedVersion(query);

        // Reuses the paging function written for the System side.
        PageResult<RubricCriterion> criteriaPage = rubricCriterionRepository.findAllByRubricVersionId(
                query.versionId(),
                query.page(),
                query.size()
        );
        return rubricCriterionDtoMapper.toPage(criteriaPage);
    }

    private void requireActiveSchoolMember(UUID schoolId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The account does not exist or is locked.");
        }

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("The account is not linked to a school."));
        if (!schoolUser.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("SECURITY: you are not allowed to access data of another school.");
        }
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found."));
        if (!school.isActive()) {
            throw new ForbiddenException("This school is disabled.");
        }
    }

    // The version must sit inside a Rubric owned by the school.
    private void requireSchoolOwnedVersion(ViewSchoolRubricCriteriaQuery query) {
        RubricVersion version = rubricVersionRepository.findById(query.versionId())
                .orElseThrow(() -> new ResourceNotFoundException("Rubric Version not found."));
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Rubric not found."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(query.schoolId())) {
            throw new ForbiddenException("Action denied: this version does not belong to your school.");
        }
    }
}