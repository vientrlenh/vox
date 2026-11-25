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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng học với ID đã cho."));

        // SECURITY GUARD 2: the room must really belong to this school
        if (!room.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Phòng học này không thuộc về trường học đã chỉ định.");
        }

        // Soft delete
        if (!room.isActive()) {
            throw new IllegalArgumentException("Phòng học này đã bị xóa (vô hiệu hóa) từ trước rồi.");
        }

        room.setActive(false); // Switch the status to false

        schoolRoomRepository.save(room);

        return null;
    }

    private void requireCallerMayDeleteRoomsOf(UUID schoolId) {
        // exists call instead of loading the user, for performance
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản của bạn không tồn tại hoặc đã bị khóa.");
        }

        // SECURITY GUARD 1: check the school user's rights
        Optional<SchoolUser> schoolUserOpt = schoolUserRepository.findByUserId(currentUserId);
        if (schoolUserOpt.isPresent()) {
            SchoolUser schoolUser = schoolUserOpt.get();
            if (!schoolUser.getSchoolId().equals(schoolId)) {
                throw new ForbiddenException("BẢO MẬT: Bạn không có quyền xóa phòng học của trường khác.");
            }
        }
    }
}