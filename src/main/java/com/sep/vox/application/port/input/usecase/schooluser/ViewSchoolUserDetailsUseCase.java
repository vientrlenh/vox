package com.sep.vox.application.port.input.usecase.schooluser;

import com.sep.vox.domain.model.school.SchoolUser;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.schooluser.ViewSchoolUserDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.mapper.SchoolUserDtoMapper;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ViewSchoolUserDetailsUseCase implements IUseCase<ViewSchoolUserDetailsQuery, SchoolUserDto> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional(readOnly = true)
    public SchoolUserDto execute(ViewSchoolUserDetailsQuery input) {
        UUID callerId = userContextPort.getCurrentAuthenticatedUserId();

        if (!userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The user status is invalid");
        }

        if (!userContextPort.isSystemAdmin() && !schoolUserRepository.existsBySchoolIdAndUserId(input.schoolId(), callerId)) {
            throw new ForbiddenException("The access permission is invalid");
        }

        // An admin may also see deactivated (DISABLED) users -- the FE has a "Show deactivated users" filter and
        // allows viewing their details. Access is already restricted to SCHOOL_ADMIN/SYSTEM_ADMIN above.
        SchoolUser schoolUser = schoolUserRepository.findBySchoolIdAndUserId(input.schoolId(), input.userId())
            .orElseThrow(() -> new ResourceNotFoundException("The requested school user was not found"));

        return SchoolUserDtoMapper.toSchoolUserDto(schoolUser);
    }
}
