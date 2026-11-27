package com.sep.vox.application.port.input.usecase.school;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.school.DeleteSchoolCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteSchoolUseCase implements IUseCase<DeleteSchoolCommand, UUID> {

    private final SchoolRepository schoolRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRoomRepository schoolRoomRepository;

    // Remember to inject the Grade and Class repositories here as well

    @Override
    @Transactional // Required so the data is locked
    public UUID execute(DeleteSchoolCommand command) {

        // Lock the school data (avoids a clash when someone is updating it)
        School school = schoolRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("No school found with the given ID."));

        requireActiveCurrentUser();

        if (!school.isActive()) {
            throw new IllegalArgumentException("This school was already deactivated (deleted).");
        }

        rejectWhenSchoolHasActiveDependencies(school);

        school.setActive(false);

        schoolRepository.save(school);

        return school.getId();
    }

    private void requireActiveCurrentUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Your account was not found."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Your account has been locked.");
        }
    }

    private void rejectWhenSchoolHasActiveDependencies(School school) {
        boolean hasActiveRooms = schoolRoomRepository.existsBySchoolIdAndIsActive(school.getId(), true);
        if (hasActiveRooms) {
            throw new IllegalArgumentException("Cannot delete. This school still has active rooms.");
        }

        boolean hasActiveGrades = schoolGradeRepository.existsBySchoolIdAndStatus(school.getId(), SchoolGradeStatus.ACTIVE.name());
        if (hasActiveGrades) {
            throw new IllegalArgumentException("Cannot delete. This school still has active grades/school years.");
        }

        boolean hasActiveClasses = schoolClassRepository.existsBySchoolIdAndStatus(school.getId(), SchoolClassStatus.ACTIVE.name());
        if (hasActiveClasses) {
            throw new IllegalArgumentException("Cannot delete. This school still has active classes.");
        }
    }
}