package com.sep.vox.application.port.input.usecase.schoolclass;

import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClass;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.schoolclass.DeleteSchoolClassCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schoolclass.DeleteSchoolClassResponse;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassDependencyRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class DeleteSchoolClassUseCase implements IUseCase<DeleteSchoolClassCommand, DeleteSchoolClassResponse> {

    private static final String HARD_DELETE = "HARD";
    private static final String SOFT_DELETE = "SOFT";

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final SchoolClassDependencyRepository schoolClassDependencyRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public DeleteSchoolClassResponse execute(DeleteSchoolClassCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = findCurrentUser(currentUserId);
        UUID schoolId = getSchoolId(currentUser);
        validateRequestedSchool(input.schoolId(), schoolId);
        validateSchool(schoolId);

        SchoolClass schoolClass = findNotYetArchivedClassOfSchool(input.id(), schoolId);

        if (!schoolClassDependencyRepository.existsDependencyBySchoolClassId(input.id())) {
            schoolClassRepository.deleteById(input.id());
            return new DeleteSchoolClassResponse(input.id(), HARD_DELETE, null, null);
        }

        return archiveClassAndDeactivateMembers(schoolClass);
    }

    private SchoolClass findNotYetArchivedClassOfSchool(UUID classId, UUID schoolId) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        if (!Objects.equals(schoolClass.getSchoolId(), schoolId)) {
            throw new ResourceNotFoundException("Class not found");
        }

        // Already soft-deleted (ARCHIVED) earlier -> treated as non-existent, to avoid archiving/deactivating again.
        if (schoolClass.getStatus() == SchoolClassStatus.ARCHIVED) {
            throw new ResourceNotFoundException("The class was already deleted");
        }
        return schoolClass;
    }

    // Soft delete: archive the class and deactivate every active member in the same transaction.
    private DeleteSchoolClassResponse archiveClassAndDeactivateMembers(SchoolClass schoolClass) {
        Instant now = Instant.now();
        schoolClassUserRepository.deactivateBySchoolClassId(schoolClass.getId(), now);
        schoolClass.setStatus(SchoolClassStatus.ARCHIVED);
        SchoolClass saved = schoolClassRepository.save(schoolClass);
        return new DeleteSchoolClassResponse(
            saved.getId(),
            SOFT_DELETE,
            saved.getStatus().name(),
            saved.getUpdatedAt().toString()
        );
    }

    private User findCurrentUser(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        validateCurrentUserIsActive(user);
        return user;
    }

    private void validateCurrentUserIsActive(User currentUser) {
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("The current user is not active");
        }
    }

    private UUID getSchoolId(User currentUser) {
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("The current user does not belong to any school"));
        return schoolUser.getSchoolId();
    }

    private void validateSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        if (!school.isActive()) {
            throw new IllegalArgumentException("The school is not active");
        }
    }
    private void validateRequestedSchool(UUID requestedSchoolId, UUID currentSchoolId) {
        if (requestedSchoolId == null) {
            throw new IllegalArgumentException("The school must not be empty");
        }
        if (!Objects.equals(requestedSchoolId, currentSchoolId)) {
            throw new IllegalArgumentException("The school does not match the current user");
        }
    }
}
