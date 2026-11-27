package com.sep.vox.application.port.input.usecase.schoolgrade;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.schoolgrade.ViewSchoolGradesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolGradeDto;

import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ViewSchoolGradesUseCase implements IUseCase<ViewSchoolGradesQuery, PageResult<SchoolGradeDto>> {

    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolGradeDto> execute(ViewSchoolGradesQuery query) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        validateUserAndAccess(currentUserId, query.schoolId());

        if (!schoolRepository.existsById(query.schoolId())) {
            throw new ResourceNotFoundException("School not found.");
        }

        // status null -> by default hides soft-deleted school years (ARCHIVED).
        // Pass status=ARCHIVED to get exactly the soft-deleted ones.
        PageResult<SchoolGrade> pageResult = schoolGradeRepository.findBySchoolId(
                query.schoolId(),
                query.gradeLevelId(),
                query.status() == null ? null : query.status().name(),
                query.page(),
                query.size()
        );

        return toDtoPage(pageResult);
    }

    private PageResult<SchoolGradeDto> toDtoPage(PageResult<SchoolGrade> pageResult) {
        return new PageResult<>(
                pageResult.content().stream()
                        .map(SchoolGradeDto::toDto)
                        .toList(),
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages()
        );
    }

    private void validateUserAndAccess(UUID userId, UUID targetSchoolId) {
        if (!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The account does not exist or has been locked.");
        }
        if (!userContextPort.isSystemAdmin()) {
            UUID userSchoolId = schoolUserRepository.findSchoolIdByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("You are not allowed to view another school's school year."));
            if (!userSchoolId.equals(targetSchoolId)) {
                throw new ForbiddenException("You are not allowed to view another school's school year.");
            }
        }
    }
}
