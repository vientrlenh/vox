package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.AddSchoolRoomCommand;
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
// Đổi kiểu trả về từ Void sang SchoolRoomResponse
public class AddSchoolRoomUseCase implements IUseCase<AddSchoolRoomCommand, SchoolRoomResponse> {

    private final SchoolRoomRepository schoolRoomRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    public AddSchoolRoomUseCase(SchoolRoomRepository schoolRoomRepository, UserContextPort userContextPort, UserRepository userRepository) {
        this.schoolRoomRepository = schoolRoomRepository;
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public SchoolRoomResponse execute(AddSchoolRoomCommand command) {
        // 1. Kiểm tra trùng lặp
        if (schoolRoomRepository.existsBySchoolIdAndCode(command.schoolId(), command.code())) {
            throw new DuplicatedException("Mã phòng học này đã tồn tại trong trường.");
        }

        // 2. Validate User & Bảo mật
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản của bạn."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        // Logic check quyền: Nếu user có schoolId (tức là Admin của 1 trường cụ thể)
        if (currentUser.getSchoolId() != null && !currentUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền xóa trường học của đơn vị khác.");
        }


        OffsetDateTime now = OffsetDateTime.now();

        // 3. Khởi tạo Domain Model
        SchoolRoom newRoom = new SchoolRoom(
                command.schoolId(),
                command.code(),
                command.name(),
                command.description(),
                false,
                now,
                now,
                currentUserId,
                currentUserId
        );

        // 4. Lưu xuống Database và Hứng lại đối tượng đã lưu
        SchoolRoom savedRoom = schoolRoomRepository.save(newRoom);

        // 5. Map sang Response và trả về cho Controller
        return new SchoolRoomResponse(
                savedRoom.getId(),
                savedRoom.getSchoolId(),
                savedRoom.getCode(),
                savedRoom.getName(),
                savedRoom.getDescription(),
                savedRoom.isActive(),
                savedRoom.getCreatedAt(),
                savedRoom.getCreatedBy(),
                savedRoom.getUpdatedAt(),
                savedRoom.getUpdatedBy()
        );
    }
}