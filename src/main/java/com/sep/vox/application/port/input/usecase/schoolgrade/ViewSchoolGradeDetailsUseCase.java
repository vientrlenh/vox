package com.sep.vox.application.port.input.usecase.schoolgrade;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.schoolgrade.ViewSchoolGradeDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolGradeDto;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ViewSchoolGradeDetailsUseCase implements IUseCase<ViewSchoolGradeDetailsQuery, SchoolGradeDto> {

    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public SchoolGradeDto execute(ViewSchoolGradeDetailsQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        validateUserAndAccess(currentUserId, input.schoolId());

        if (!schoolRepository.existsById(input.schoolId())) {
            throw new ResourceNotFoundException("School not found.");
        }

        SchoolGrade grade = schoolGradeRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("School year/course not found."));

        // The school year stores schoolId directly -- no need to go through the Grade to find the owning school.
        if (!grade.getSchoolId().equals(input.schoolId())) {
            throw new ResourceNotFoundException("School year/course not found.");
        }

        return SchoolGradeDto.toDto(grade);
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
