package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.schoolroom.AddSchoolRoomCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
@Service
@RequiredArgsConstructor
public class AddSchoolRoomUseCase implements IUseCase<AddSchoolRoomCommand, UUID> {

    private final SchoolRoomRepository schoolRoomRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UUID execute(AddSchoolRoomCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        validateUserAndAccess(currentUserId, command.schoolId());

        if (!schoolRepository.existsById(command.schoolId())) {
            throw new ResourceNotFoundException("School not found.");
        }

        String safeCode = StringNormalization.normalizeCode(command.code());
        if (schoolRoomRepository.existsBySchoolIdAndCode(command.schoolId(), safeCode)) {
            throw new ResourceDuplicatedException("The room code already exists in the school.");
        }

        SchoolRoom savedRoom = saveRoom(command, safeCode, currentUserId);

        return savedRoom.getId();
    }

    private void validateUserAndAccess(UUID userId, UUID targetSchoolId) {
        if (!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The account does not exist or is locked.");
        }

        // A System Admin (who has no SchoolUser) may pass.
        // A School Admin must match the schoolId.
        schoolUserRepository.findSchoolIdByUserId(userId).ifPresent(userSchoolId -> {
            if (!userSchoolId.equals(targetSchoolId)) {
                throw new ForbiddenException("You are not allowed to manage this school.");
            }
        });
    }

    private SchoolRoom saveRoom(AddSchoolRoomCommand command, String code, UUID creatorId) {
        SchoolRoom newRoom = SchoolRoom.registered(
            command.schoolId(),
            code,
            StringNormalization.trimAndCollapseSpaces(command.name()),
            StringNormalization.trimAndCollapseSpaces(command.description()));
        return schoolRoomRepository.save(newRoom);
    }
}