package com.sep.vox.application.port.input.usecase.schoolgradelevel;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteSchoolGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DeleteSchoolGradeLevelUseCase implements IUseCase<DeleteSchoolGradeLevelCommand, Void> {

    private final SchoolGradeLevelRepository schoolGradeLevelRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public DeleteSchoolGradeLevelUseCase(
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            SchoolGradeRepository schoolGradeRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteSchoolGradeLevelCommand command) {

        // 1. Validate User
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }

        // VÒNG 1: KIỂM TRA QUYỀN SCHOOL USER (system admin bỏ qua)
        if (!userContextPort.isSystemAdmin()) {
            SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("BẢO MẬT: Bạn không có quyền xóa dữ liệu của trường khác."));
            if (!schoolUser.getSchoolId().equals(command.schoolId())) {
                throw new ForbiddenException("BẢO MẬT: Bạn không có quyền xóa dữ liệu của trường khác.");
            }
        }

        // 2. Lấy Khối (GradeLevel) cần xóa
        SchoolGradeLevel gradeLevel = schoolGradeLevelRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Khối học sinh với ID đã cho."));

        // VÒNG 2: KIỂM TRA TÍNH SỞ HỮU
        if (!gradeLevel.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Khối này không thuộc quyền quản lý của trường bạn.");
        }
        // ====================================================================

        // 3. Nếu Khối đã bị xóa mềm trước đó (INACTIVE) thì coi như không còn tồn tại.
        //    KHÔNG deleteById ở đây: cùng nằm trong @Transactional nên throw sẽ rollback
        //    và xóa cứng không bao giờ thực sự xảy ra (bug cũ).
        if (gradeLevel.getStatus() != SchoolGradeLevelStatus.ACTIVE) {
            throw new NotFoundException("Không tìm thấy khối học.");
        }

        // 4. Logic chặn xóa (Toàn vẹn dữ liệu): chỉ chặn khi còn Năm học CHƯA bị xóa mềm.
        boolean isUsed = schoolGradeRepository.existsBySchoolGradeLevelIdAndStatusNot(
                gradeLevel.getId(), SchoolGradeStatus.ARCHIVED.name());
        if (isUsed) {
            throw new IllegalStateException("Không thể xóa vì Khối này đang chứa các Năm học/Khóa học bên trong.");
        }

        // 5. Xóa mềm: chuyển sang INACTIVE.
        gradeLevel.setStatus(SchoolGradeLevelStatus.INACTIVE);
        gradeLevel.setUpdatedAt(OffsetDateTime.now());
        gradeLevel.setUpdatedBy(currentUserId);
        schoolGradeLevelRepository.save(gradeLevel);

        return null;
    }
}