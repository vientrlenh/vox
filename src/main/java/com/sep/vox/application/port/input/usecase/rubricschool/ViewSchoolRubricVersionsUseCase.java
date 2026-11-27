package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.rubricschool.ViewSchoolRubricVersionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricVersionDto;
import com.sep.vox.domain.mapper.RubricVersionDtoMapper;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class ViewSchoolRubricVersionsUseCase implements IUseCase<ViewSchoolRubricVersionsQuery, PageResult<RubricVersionDto>> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<RubricVersionDto> execute(ViewSchoolRubricVersionsQuery query) {
        requireActiveSchoolMember(query.schoolId());
        requireSchoolOwnedRubric(query);

        String safeStatus = query.status() == null ? null : query.status().name();
        PageResult<RubricVersion> versionPage = rubricVersionRepository.findAllByRubricIdAndStatus(
                query.rubricId(),
                safeStatus,
                query.page(),
                query.size()
        );
        return RubricVersionDtoMapper.toRubricVersionPage(versionPage);
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

    // The root Rubric must belong to the school being accessed.
    private void requireSchoolOwnedRubric(ViewSchoolRubricVersionsQuery query) {
        Rubric rubric = rubricRepository.findById(query.rubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Rubric not found."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(query.schoolId())) {
            throw new ForbiddenException("Action denied: this Rubric does not belong to your school.");
        }
    }
}