package com.sep.vox.application.port.input.usecase.school;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteSchoolCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.SchoolResponse.SchoolResponse;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteSchoolUseCase implements IUseCase<DeleteSchoolCommand, SchoolResponse> {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public DeleteSchoolUseCase(SchoolRepository schoolRepository, UserRepository userRepository, UserContextPort userContextPort) {
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SchoolResponse execute(@NonNull DeleteSchoolCommand command) {
        // 1. Kiểm tra trường học có tồn tại không
        School school = schoolRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học với ID đã cho."));

        // 2. Kiểm tra trường học có đang hoạt động không.
        if (school.isActive()) {
            throw new IllegalStateException("Trường học đang hoạt động. Vui lòng vô hiệu hóa trường học trước khi xóa vĩnh viễn.");
        }

        // 3. Lấy thông tin người dùng đang thực hiện xóa
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản của bạn."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        // 4. MAP DỮ LIỆU SANG RESPONSE (Chụp lại khoảnh khắc trước khi xóa)
        SchoolResponse response = new SchoolResponse(
                school.getId(),
                school.getCode().value(),
                school.getName(),
                school.getDescription(),
                school.getContactPhone().value(),
                school.getContactEmail().value(),
                school.getDomain().value(),
                school.getAddress(),
                school.getStudentCount().value(),
                school.isActive(),
                school.getCreatedAt(),
                school.getCreatedBy(),
                school.getUpdatedAt(),
                school.getUpdatedBy()
        );

        // 5. [XÓA CỨNG]: XÓA KHỎI DATABASE
        schoolRepository.deleteById(school.getId());

        // 6. Trả về thông tin trường học vừa bị xóa
        return response;
    }
}