package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteSchoolRoomCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.SchoolRoomResponse.SchoolRoomResponse;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DeleteSchoolRoomUseCase implements IUseCase<DeleteSchoolRoomCommand, SchoolRoomResponse> {

    private final SchoolRoomRepository schoolRoomRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public DeleteSchoolRoomUseCase(SchoolRoomRepository schoolRoomRepository, UserRepository userRepository, UserContextPort userContextPort) {
        this.schoolRoomRepository = schoolRoomRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SchoolRoomResponse execute(DeleteSchoolRoomCommand command) {

        // 1. GỌI HÀM LOCK: Khóa an toàn giống hệt Update
        SchoolRoom room = schoolRoomRepository.findByIdForUpdate(command.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phòng học với ID đã cho."));

        // 2. Validate User
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản của bạn."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        // 3. THỰC HIỆN XÓA MỀM (SOFT DELETE)
        if (!room.isActive()) {
            throw new IllegalStateException("Phòng học này đã bị xóa (vô hiệu hóa) từ trước rồi.");
        }

        room.setActive(false); // Chuyển trạng thái thành false
        room.setUpdatedBy(currentUserId); // Ghi nhận ID của người vừa bấm nút xóa
        room.setUpdatedAt(OffsetDateTime.now()); // Thời điểm bị xóa

        // 4. LƯU LẠI VÀO DB
        SchoolRoom deletedRoom = schoolRoomRepository.save(room);

        // Trả về ID
        return new SchoolRoomResponse(
                room.getId(),
                room.getSchoolId(),
                room.getCode(),
                room.getName(),
                room.getDescription(),
                room.isActive(),
                room.getCreatedAt(),
                room.getCreatedBy(),
                room.getUpdatedAt(),
                room.getUpdatedBy()
        );
    }
}