package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSchoolRoomCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UpdateSchoolRoomUseCase implements IUseCase<UpdateSchoolRoomCommand, UUID> {

    private final SchoolRoomRepository schoolRoomRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public UpdateSchoolRoomUseCase(
            SchoolRoomRepository schoolRoomRepository,
            SchoolUserRepository schoolUserRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.schoolRoomRepository = schoolRoomRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSchoolRoomCommand command) {

        // 1. Validate User (Tối ưu bằng hàm exists)
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản của bạn không tồn tại hoặc đã bị khóa.");
        }

        // 2. Tìm phòng học cần sửa
        SchoolRoom room = schoolRoomRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phòng học với ID đã cho."));

        // VÒNG BẢO MẬT: KIỂM TRA QUYỀN SCHOOL USER
        Optional<SchoolUser> schoolUserOpt = schoolUserRepository.findByUserId(currentUserId);
        if (schoolUserOpt.isPresent()) {
            SchoolUser schoolUser = schoolUserOpt.get();
            if (!schoolUser.getSchoolId().equals(room.getSchoolId())) {
                // Sửa lại câu thông báo cho đúng với Phòng học
                throw new ForbiddenException("BẢO MẬT: Bạn không có quyền sửa thông tin phòng học của trường khác.");
            }
        }

        // 3. Chuẩn hóa & validate dữ liệu đầu vào
        String safeName = (command.name() != null) ? StringNormalization.trimAndCollapseSpaces(command.name()) : null;
        String safeDesc = (command.description() != null) ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;

        if (command.capacity() != null && command.capacity() < 1) {
            throw new IllegalArgumentException("Sức chứa phòng phải lớn hơn hoặc bằng 1.");
        }

        // 4. Thực thi Atomic Update
        // Hàm Atomic Update ở Repository của bạn đã dùng COALESCE, nên ta cứ truyền thẳng chuỗi đã chuẩn hóa xuống.
        int updatedRows = schoolRoomRepository.updateSchoolRoomAtomic(
                command.id(),
                safeName,
                safeDesc,
                command.capacity(),
                OffsetDateTime.now(),
                currentUserId
        );

        if (updatedRows == 0) {
            throw new NotFoundException("Cập nhật thất bại hoặc không có thay đổi nào.");
        }

        // Trả về ID của phòng học vừa được update mới là chuẩn bài
        return command.id();
    }
}