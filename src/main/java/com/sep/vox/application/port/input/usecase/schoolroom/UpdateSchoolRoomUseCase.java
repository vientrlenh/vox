package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.schoolroom.UpdateSchoolRoomCommand;
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
public class UpdateSchoolRoomUseCase implements IUseCase<UpdateSchoolRoomCommand, UUID> {

    private final SchoolRoomRepository schoolRoomRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(UpdateSchoolRoomCommand command) {

        // exists call instead of loading the user, for performance
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Your account does not exist or has been locked.");
        }

        SchoolRoom room = schoolRoomRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("No room found with the given ID."));

        requireSameSchoolForSchoolUser(currentUserId, room);

        int updatedRows = updateAtomically(command);

        if (updatedRows == 0) {
            throw new ResourceNotFoundException("Update failed or there was no change.");
        }

        return command.id();
    }

    private void requireSameSchoolForSchoolUser(UUID currentUserId, SchoolRoom room) {
        // SECURITY GUARD: check the school user's rights
        Optional<SchoolUser> schoolUserOpt = schoolUserRepository.findByUserId(currentUserId);
        if (schoolUserOpt.isPresent()) {
            SchoolUser schoolUser = schoolUserOpt.get();
            if (!schoolUser.getSchoolId().equals(room.getSchoolId())) {
                throw new ForbiddenException("SECURITY: You are not allowed to edit another school's room information.");
            }
        }
    }

    // The repository statement keeps a column when its value is null (COALESCE) and clears the description only when it is provided.
    private int updateAtomically(UpdateSchoolRoomCommand command) {
        String safeName = (command.name() != null) ? StringNormalization.trimAndCollapseSpaces(command.name()) : null;
        String safeDesc = (command.description() != null) ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;
        return schoolRoomRepository.updateSchoolRoomAtomic(
                command.id(),
                safeName,
                safeDesc,
                command.descriptionProvided()
        );
    }
}