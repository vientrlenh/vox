package com.sep.vox.application.port.input.usecase.rubricteacher;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.rubricteacher.SearchTeacherRubricsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricDto;
import com.sep.vox.domain.mapper.RubricDtoMapper;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchTeacherRubricsUseCase implements IUseCase<SearchTeacherRubricsQuery, PageResult<RubricDto>> {

    private final RubricRepository rubricRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<RubricDto> execute(SearchTeacherRubricsQuery query) {
        SchoolUser schoolUser = loadSchoolUserOfCurrentTeacher();

        // The repository builds the dynamic filter SQL and returns only the rubrics of the teacher's school.
        PageResult<Rubric> pageResult = rubricRepository.searchSchoolRubrics(
                schoolUser.getSchoolId(),
                query.keyword(),
                query.frameworkId(),
                query.languageId(),
                query.page(),
                query.size()
        );
        return new PageResult<>(
                pageResult.content().stream()
                        .map(RubricDtoMapper::toRubricDto)
                        .toList(),
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages()
        );
    }

    // The teacher's school comes from SchoolUser; the client never sends a schoolId.
    private SchoolUser loadSchoolUserOfCurrentTeacher() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Your account has been locked.");
        }

        return schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Your account is not linked to any school."));
    }
}
