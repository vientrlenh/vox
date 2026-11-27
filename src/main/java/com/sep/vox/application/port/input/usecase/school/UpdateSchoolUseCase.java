package com.sep.vox.application.port.input.usecase.school;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.school.UpdateSchoolCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateSchoolUseCase implements IUseCase<UpdateSchoolCommand, UUID> {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(UpdateSchoolCommand command) {
        School school = schoolRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("School not found."));

        // exists call instead of loading the user, for performance
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The account does not exist or has been locked.");
        }
        requireSameSchoolForSchoolUser(currentUserId, school);

        int updatedRows = updateAtomically(command, normalizedUniquePhone(command), normalizedUniqueEmail(command));

        if (updatedRows == 0) {
            throw new ResourceNotFoundException("Update failed or there was no change.");
        }

        return command.id();
    }

    private int updateAtomically(UpdateSchoolCommand command, String phone, String email) {
        return schoolRepository.updateSchoolAtomic(
                command.id(),
                normalizedText(command.name()),
                normalizedText(command.description()),
                command.descriptionProvided(),
                phone,
                email,
                command.domain() != null ? StringNormalization.normalizeDomain(command.domain()) : null,
                command.domainProvided(),
                normalizedText(command.address()),
                command.studentCount()
        );
    }

    private void requireSameSchoolForSchoolUser(UUID currentUserId, School school) {
        // SECURITY GUARD: a user linked to a school (a school admin) may only edit their own school
        Optional<SchoolUser> schoolUserOpt = schoolUserRepository.findByUserId(currentUserId);
        if (schoolUserOpt.isPresent() && !schoolUserOpt.get().getSchoolId().equals(school.getId())) {
            throw new ForbiddenException("SECURITY: You are not allowed to edit the information of another organization's school.");
        }
    }

    private String normalizedText(String value) {
        return value != null ? StringNormalization.trimAndCollapseSpaces(value) : null;
    }

    private String normalizedUniquePhone(UpdateSchoolCommand command) {
        if (command.contactPhone() == null) {
            return null;
        }
        String phone = StringNormalization.normalizePhone(command.contactPhone());
        if (schoolRepository.existsByContactPhoneAndIdNot(phone, command.id())) {
            throw new ResourceDuplicatedException("This phone number is already in use.");
        }
        return phone;
    }

    private String normalizedUniqueEmail(UpdateSchoolCommand command) {
        if (command.contactEmail() == null) {
            return null;
        }
        String email = StringNormalization.normalizeEmail(command.contactEmail());
        if (schoolRepository.existsByContactEmailAndIdNot(email, command.id())) {
            throw new ResourceDuplicatedException("This email is already in use.");
        }
        return email;
    }
}
