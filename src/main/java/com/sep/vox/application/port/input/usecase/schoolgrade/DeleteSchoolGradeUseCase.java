package com.sep.vox.application.port.input.usecase.schoolgrade;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteSchoolGradeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DeleteSchoolGradeUseCase implements IUseCase<DeleteSchoolGradeCommand, Void> {

    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolGradeLevelRepository schoolGradeLevelRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;

    public DeleteSchoolGradeUseCase(
            SchoolGradeRepository schoolGradeRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            UserContextPort userContextPort,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository
    ) {
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public Void execute(DeleteSchoolGradeCommand command) {
        // 1. Xác định User hiện tại
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        //2 kiểm tra bằng school user
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn chưa liên kết với trường học."));

        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền thao tác trên dữ liệu của trường khác.");
        }

        // 2. Lấy School Grade (Năm học/Khóa học) cần xóa
        SchoolGrade grade = schoolGradeRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy năm học/khóa học này."));

        // 3. Lấy School Grade Level làm "cầu nối" để truy ra School ID
        var gradeLevel = schoolGradeLevelRepository.findById(grade.getSchoolGradeLevelId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Khối Lớp chứa năm học này."));


        if (!gradeLevel.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Dữ liệu này không thuộc quyền quản lý của trường bạn.");
        }

        // 4. Logic nghiệp vụ: Chặn xóa nếu đang sử dụng
        if (grade.getStatus() == SchoolGradeStatus.ARCHIVED) {
            throw new IllegalStateException("Dữ liệu này đã được lưu trữ (xóa mềm) từ trước.");
        }

        boolean isUsed = schoolClassRepository.existsBySchoolGradeId(grade.getId());
        if (isUsed) {
            throw new IllegalStateException("Không thể xóa vì đang có Lớp học (Class) sử dụng năm học này.");
        }

        // 5. Thực hiện Xóa
        if (grade.getStatus() == SchoolGradeStatus.ACTIVE) {
            grade.setStatus(SchoolGradeStatus.ARCHIVED);
            grade.setUpdatedAt(OffsetDateTime.now());
            grade.setUpdatedBy(currentUserId);
            schoolGradeRepository.save(grade); // Xóa mềm
        } else if (grade.getStatus() == SchoolGradeStatus.INACTIVE) {
            schoolGradeRepository.deleteById(grade.getId()); // Xóa cứng
        }

        // 6. Trả về kết quả
        return null;
    }
}