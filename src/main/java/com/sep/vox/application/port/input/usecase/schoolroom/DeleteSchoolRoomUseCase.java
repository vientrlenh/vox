package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteSchoolRoomCommand;
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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeleteSchoolRoomUseCase implements IUseCase<DeleteSchoolRoomCommand, Void> {

    private final SchoolRoomRepository schoolRoomRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public DeleteSchoolRoomUseCase(
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
    public Void execute(DeleteSchoolRoomCommand command) {

        // 1. Validate User (Tối ưu bằng hàm exists)
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản của bạn không tồn tại hoặc đã bị khóa.");
        }

        // VÒNG BẢO MẬT 1: KIỂM TRA QUYỀN SCHOOL USER
        Optional<SchoolUser> schoolUserOpt = schoolUserRepository.findByUserId(currentUserId);
        if (schoolUserOpt.isPresent()) {
            SchoolUser schoolUser = schoolUserOpt.get();
            if (!schoolUser.getSchoolId().equals(command.schoolId())) {
                // Sửa lại câu báo lỗi cho đúng ngữ cảnh Phòng học
                throw new ForbiddenException("BẢO MẬT: Bạn không có quyền xóa phòng học của trường khác.");
            }
        }


        // 2. Tìm phòng học cần xóa
        SchoolRoom room = schoolRoomRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phòng học với ID đã cho."));

        // VÒNG BẢO MẬT 2: KIỂM TRA PHÒNG HỌC CÓ ĐÚNG CỦA TRƯỜNG NÀY KHÔNG
        if (!room.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Phòng học này không thuộc về trường học đã chỉ định.");
        }


        // 3. THỰC HIỆN XÓA MỀM (SOFT DELETE)
        if (!room.isActive()) {
            throw new IllegalStateException("Phòng học này đã bị xóa (vô hiệu hóa) từ trước rồi.");
        }

        room.setActive(false); // Chuyển trạng thái thành false
        room.setUpdatedBy(currentUserId); // Ghi nhận ID của người vừa bấm nút xóa
        room.setUpdatedAt(Instant.now()); // Thời điểm bị xóa

        // 4. LƯU LẠI VÀO DB
        schoolRoomRepository.save(room);

        // 5. Trả về dữ liệu mới
        return null;
    }
}