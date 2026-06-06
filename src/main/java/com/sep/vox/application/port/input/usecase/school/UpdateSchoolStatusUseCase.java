package com.sep.vox.application.port.input.usecase.school;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSchoolStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.SchoolResponse.SchoolResponse;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UpdateSchoolStatusUseCase implements IUseCase<UpdateSchoolStatusCommand, SchoolResponse> {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public UpdateSchoolStatusUseCase(SchoolRepository schoolRepository, UserRepository userRepository, UserContextPort userContextPort) {
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SchoolResponse execute(UpdateSchoolStatusCommand command) {
        // 1. Kiểm tra trường học có tồn tại không
        School school = schoolRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học với ID đã cho."));

        // 2. Lấy thông tin người dùng đang thực hiện thay đổi
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản của bạn."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        // 3. Validate Logic: Tránh cập nhật thừa
        if (school.isActive() == command.isActive()) {
            throw new IllegalStateException("Trường học này đã ở trạng thái " + (command.isActive() ? "Hoạt động" : "Vô hiệu hóa") + ". Không cần cập nhật.");
        }

        // 4. Thay đổi trạng thái
        school.setActive(command.isActive());
        school.setUpdatedBy(currentUserId);
        school.setUpdatedAt(OffsetDateTime.now());

        // 5. Lưu DB
        School updatedSchool = schoolRepository.save(school);

        // 6. Map sang Response
        return new SchoolResponse(
                updatedSchool.getId(),
                updatedSchool.getCode().value(),
                updatedSchool.getName(),
                updatedSchool.getDescription(),
                updatedSchool.getContactPhone().value(),
                updatedSchool.getContactEmail().value(),
                updatedSchool.getDomain().value(),
                updatedSchool.getAddress(),
                updatedSchool.getStudentCount().value(),
                updatedSchool.isActive(),
                updatedSchool.getCreatedAt(),
                updatedSchool.getCreatedBy(),
                updatedSchool.getUpdatedAt(),
                updatedSchool.getUpdatedBy()
        );
    }
}