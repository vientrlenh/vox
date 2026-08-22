package com.sep.vox.application.port.input.usecase.gradelevel;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class DeleteGradeLevelUseCase implements IUseCase<DeleteGradeLevelCommand, Void> {

    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public DeleteGradeLevelUseCase(
            GradeLevelRepository gradeLevelRepository,
            SchoolGradeRepository schoolGradeRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.gradeLevelRepository = gradeLevelRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteGradeLevelCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Chỉ quản trị hệ thống mới được xóa khối học.");
        }

        GradeLevel gradeLevel = gradeLevelRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Khối học sinh với ID đã cho."));

        // Nếu Khối đã bị xóa mềm trước đó (INACTIVE) thì coi như không còn tồn tại.
        // KHÔNG deleteById ở đây: cùng nằm trong @Transactional nên throw sẽ rollback
        // và xóa cứng không bao giờ thực sự xảy ra (bug cũ).
        if (gradeLevel.getStatus() != GradeLevelStatus.ACTIVE) {
            throw new NotFoundException("Không tìm thấy khối học.");
        }

        // Chặn xóa khi còn Năm học CHƯA bị xóa mềm. Catalog giờ dùng chung nên phép kiểm tra này
        // quét MỌI trường -- một trường còn dùng là đủ để chặn.
        boolean isUsed = schoolGradeRepository.existsByGradeLevelIdAndStatusNot(
                gradeLevel.getId(), SchoolGradeStatus.ARCHIVED.name());
        if (isUsed) {
            throw new IllegalStateException("Không thể xóa vì Khối này đang được các trường sử dụng.");
        }

        // Xóa mềm: chuyển sang INACTIVE.
        gradeLevel.setStatus(GradeLevelStatus.INACTIVE);
        gradeLevel.setUpdatedAt(Instant.now());
        gradeLevel.setUpdatedBy(currentUserId);
        gradeLevelRepository.save(gradeLevel);

        return null;
    }
}
