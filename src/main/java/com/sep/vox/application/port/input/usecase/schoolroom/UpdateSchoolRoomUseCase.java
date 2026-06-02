package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSchoolRoomCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.SchoolRoomResponse.SchoolRoomResponse;
import com.sep.vox.domain.model.schoolroom.SchoolRoom;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UpdateSchoolRoomUseCase implements IUseCase<UpdateSchoolRoomCommand, SchoolRoomResponse> {

    private final SchoolRoomRepository schoolRoomRepository;
    private final UserContextPort userContextPort;

    public UpdateSchoolRoomUseCase(SchoolRoomRepository schoolRoomRepository, UserContextPort userContextPort) {
        this.schoolRoomRepository = schoolRoomRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SchoolRoomResponse execute(UpdateSchoolRoomCommand command) {
        // 1. Tìm phòng học trong Database
        SchoolRoom room = schoolRoomRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phòng học này."));

        // 2.  Đảm bảo phòng học này thực sự thuộc về schoolId
        if (!room.getSchoolId().equals(command.schoolId())) {
            throw new UnauthorizedException("Phòng học này không thuộc về trường học được chỉ định.");
        }

        // 3. Kiểm tra trùng lặp: Nếu mã (code) bị đổi, check xem code mới đã tồn tại chưa
        if (!room.getCode().equals(command.code()) &&
                schoolRoomRepository.existsBySchoolIdAndCode(command.schoolId(), command.code())) {
            throw new DuplicatedException("Mã phòng học mới này đã tồn tại trong trường.");
        }

        // 4. Lấy ID người dùng để ghi log Audit
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();


//        room.setSchoolId(command.schoolId());
        room.setCode(command.code());
        room.setName(command.name());
        room.setDescription(command.description());
        room.setUpdatedBy(currentUserId);
        room.setUpdatedAt(OffsetDateTime.now());
        room.setActive(command.isActive());


        // 6. Lưu xuống DB và trả về kết quả
        SchoolRoom savedRoom = schoolRoomRepository.save(room);

        return new SchoolRoomResponse(
                savedRoom.getId(),
                savedRoom.getSchoolId(),
                savedRoom.getCode(),
                savedRoom.getName(),
                savedRoom.getDescription(),
                savedRoom.isActive()
        );
    }
}