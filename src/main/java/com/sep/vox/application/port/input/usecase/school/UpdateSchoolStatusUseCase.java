package com.sep.vox.application.port.input.usecase.school;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSchoolStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class UpdateSchoolStatusUseCase implements IUseCase<UpdateSchoolStatusCommand, UUID> {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository; // Bổ sung Repository này
    private final UserContextPort userContextPort;

    public UpdateSchoolStatusUseCase(
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSchoolStatusCommand command) {
        // 1. Kiểm tra trường học có tồn tại không
        School school = schoolRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học với ID đã cho."));

        // 2. Validate User & Bảo mật
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản của bạn không tồn tại hoặc đã bị khóa.");
        }

        // VÒNG BẢO MẬT: KIỂM TRA BẰNG SCHOOL USER
        Optional<SchoolUser> schoolUserOpt = schoolUserRepository.findByUserId(currentUserId);

        // Nếu user có liên kết với một trường học (tức là School Admin)
        if (schoolUserOpt.isPresent()) {
            SchoolUser schoolUser = schoolUserOpt.get();
            if (!schoolUser.getSchoolId().equals(school.getId())) {
                throw new ForbiddenException("BẢO MẬT: Bạn không có quyền cập nhật trạng thái trường học của đơn vị khác.");
            }
        }

        // 3. Validate Logic: Tránh cập nhật thừa
        if (school.isActive() == command.isActive()) {
            throw new IllegalStateException("Trường học này đã ở trạng thái " + (command.isActive() ? "Hoạt động" : "Vô hiệu hóa") + ". Không cần cập nhật.");
        }

        // 4. Thay đổi trạng thái
        school.setActive(command.isActive());
        school.setUpdatedBy(currentUserId);
        school.setUpdatedAt(Instant.now());

        // 5. Lưu DB
        School updatedSchool = schoolRepository.save(school);

        // 6. Map sang Response
        return updatedSchool.getId();
    }
}