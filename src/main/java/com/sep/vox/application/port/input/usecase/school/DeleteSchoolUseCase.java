package com.sep.vox.application.port.input.usecase.school;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteSchoolCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DeleteSchoolUseCase implements IUseCase<DeleteSchoolCommand, UUID> {

    private final SchoolRepository schoolRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRoomRepository schoolRoomRepository;

    // Nhớ inject thêm 2 cái repo của Grade và Class vào đây nha
    public DeleteSchoolUseCase(
            SchoolRepository schoolRepository,
            SchoolGradeRepository schoolGradeRepository,
            SchoolClassRepository schoolClassRepository,
            UserRepository userRepository,
            UserContextPort userContextPort, SchoolRoomRepository schoolRoomRepository) {
        this.schoolRepository = schoolRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.schoolRoomRepository = schoolRoomRepository;
    }

    @Override
    @Transactional // Bắt buộc có để Lock dữ liệu
    public UUID execute(DeleteSchoolCommand command) {

        // 1. Lock dữ liệu trường học (Tránh đụng độ khi có người đang update)
        School school = schoolRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học với ID đã cho."));

        // 2. Validate User & Bảo mật
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản của bạn."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

//        // Logic check quyền: Nếu user có schoolId (tức là Admin của 1 trường cụ thể)
//        if (currentUser.getSchoolId() != null && !currentUser.getSchoolId().equals(school.getId())) {
//            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền xóa trường học của đơn vị khác.");
//        }

        // 3. Logic Xóa Mềm
        if (!school.isActive()) {
            throw new IllegalStateException("Trường học này đã bị vô hiệu hóa (xóa) từ trước rồi.");
        }

        // 4. KIỂM TRA RÀNG BUỘC (Dependencies Check)
        // check xem có room nào của trường đang activce không
        boolean hasActiveRooms = schoolRoomRepository.existsBySchoolIdAndIsActive(school.getId(), true);
        if (hasActiveRooms) {
            throw new IllegalStateException("Không thể xóa. Trường này vẫn còn Phòng học đang hoạt động.");
        }

        // Check xem có khối (Grade) nào đang ACTIVE không
        boolean hasActiveGrades = schoolGradeRepository.existsBySchoolIdAndStatus(school.getId(), SchoolGradeStatus.ACTIVE.name());
        if (hasActiveGrades) {
            throw new IllegalStateException("Không thể xóa. Trường này vẫn còn Khối/Năm học đang hoạt động.");
        }

        // Check xem có lớp (Class) nào đang ACTIVE không
        boolean hasActiveClasses = schoolClassRepository.existsBySchoolIdAndStatus(school.getId(), SchoolClassStatus.ACTIVE.name());
        if (hasActiveClasses) {
            throw new IllegalStateException("Không thể xóa. Trường này vẫn còn Lớp học đang hoạt động.");
        }



        // 5. THỰC HIỆN XÓA MỀM VÀ LƯU DB
        school.setActive(false);
        school.setUpdatedAt(OffsetDateTime.now());
        school.setUpdatedBy(currentUserId);

        schoolRepository.save(school);

        // 6. Nhả về mỗi ID
        return school.getId();
    }
}