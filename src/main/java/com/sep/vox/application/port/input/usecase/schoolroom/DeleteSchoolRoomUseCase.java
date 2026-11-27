package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.schoolroom.DeleteSchoolRoomCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteSchoolRoomUseCase implements IUseCase<DeleteSchoolRoomCommand, Void> {

    private final SchoolRoomRepository schoolRoomRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(DeleteSchoolRoomCommand command) {

        requireCallerMayDeleteRoomsOf(command.schoolId());

        SchoolRoom room = schoolRoomRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("No room found with the given ID."));

        // SECURITY GUARD 2: the room must really belong to this school
        if (!room.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("SECURITY: This room does not belong to the specified school.");
        }

        // Soft delete
        if (!room.isActive()) {
            throw new IllegalArgumentException("This room was already deleted (deactivated).");
        }

        room.setActive(false); // Switch the status to false

        schoolRoomRepository.save(room);

        return null;
    }

    private void requireCallerMayDeleteRoomsOf(UUID schoolId) {
        // exists call instead of loading the user, for performance
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Your account does not exist or has been locked.");
        }

        // SECURITY GUARD 1: check the school user's rights
        Optional<SchoolUser> schoolUserOpt = schoolUserRepository.findByUserId(currentUserId);
        if (schoolUserOpt.isPresent()) {
            SchoolUser schoolUser = schoolUserOpt.get();
            if (!schoolUser.getSchoolId().equals(schoolId)) {
                throw new ForbiddenException("SECURITY: You are not allowed to delete another school's room.");
            }
        }
    }
}