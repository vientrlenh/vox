package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSchoolRoomCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
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
public class UpdateSchoolRoomUseCase implements IUseCase<UpdateSchoolRoomCommand, UUID> {

    private final SchoolRoomRepository schoolRoomRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public UpdateSchoolRoomUseCase(SchoolRoomRepository schoolRoomRepository, UserRepository userRepository, UserContextPort userContextPort) {
        this.schoolRoomRepository = schoolRoomRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSchoolRoomCommand command) {

        // 1. GỌI HÀM LOCK: Đóng băng dòng dữ liệu này dưới Database
        SchoolRoom room = schoolRoomRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phòng học với ID đã cho."));

        // 2. Validate User & Bảo mật
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản của bạn."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        // Logic check quyền: Nếu user có schoolId (tức là Admin của 1 trường cụ thể)
        if (currentUser.getSchoolId() != null && !currentUser.getSchoolId().equals(room.getSchoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền sửa trường học của đơn vị khác.");
        }

        // 3. CẬP NHẬT PARTIAL VÀO OBJECT
        if (command.name() != null) {
            room.setName(StringNormalization.trimAndCollapseSpaces(command.name()));
        }
        if (command.description() != null) {
            room.setDescription(StringNormalization.trimAndCollapseSpaces(command.description()));
        }

        // 4. Thực thi Atomic Update
        int updatedRows = schoolRoomRepository.updateSchoolRoomAtomic(
                command.id(),
                room.getName(),
                room.getDescription(),
                OffsetDateTime.now(),
                currentUserId
        );

        if (updatedRows == 0) {
            throw new NotFoundException("Cập nhật thất bại.");
        }

        return room.getSchoolId(); // Trả về schoolId theo yêu cầu của bạn
    }
}