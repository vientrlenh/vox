package com.sep.vox.application.port.input.usecase.school;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSchoolCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
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
public class UpdateSchoolUseCase implements IUseCase<UpdateSchoolCommand, UUID> {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public UpdateSchoolUseCase(SchoolRepository schoolRepository, UserRepository userRepository, UserContextPort userContextPort) {
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSchoolCommand command) {
        // 1. Validate sự tồn tại và quyền truy cập (Giữ nguyên vì đây là bước bảo mật quan trọng)
        School school = schoolRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        if (currentUser.getSchoolId() != null && !currentUser.getSchoolId().equals(school.getId())) {
            throw new UnauthorizedException("Bạn không có quyền sửa trường này.");
        }

        // 2. Chuẩn hóa dữ liệu trước khi gửi xuống DB
        String name = (command.name() != null) ? StringNormalization.trimAndCollapseSpaces(command.name()) : null;
        String description = (command.description() != null) ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;
        String address = (command.address() != null) ? StringNormalization.trimAndCollapseSpaces(command.address()) : null;

        String phone = null;
        if (command.contactPhone() != null) {
            phone = StringNormalization.normalizePhone(command.contactPhone());
            if (schoolRepository.existsByContactPhoneAndIdNot(phone, command.id())) {
                throw new DuplicatedException("Số điện thoại này đã được sử dụng.");
            }
        }

        String email = null;
        if (command.contactEmail() != null) {
            email = StringNormalization.normalizeEmail(command.contactEmail());
            if (schoolRepository.existsByContactEmailAndIdNot(email, command.id())) {
                throw new DuplicatedException("Email này đã được sử dụng.");
            }
        }

        String domain = null;
        if (command.domain() != null) {
            domain = StringNormalization.normalizeDomain(command.domain());
            if (schoolRepository.existsByDomainAndIdNot(domain, command.id())) {
                throw new DuplicatedException("Tên miền này đã được sử dụng.");
            }
        }

        // 3. Thực thi Atomic Update
        int updatedRows = schoolRepository.updateSchoolAtomic(
                command.id(),
                name,
                description,
                phone,
                email,
                domain,
                address,
                command.studentCount(),
                OffsetDateTime.now(),
                currentUserId
        );

        if (updatedRows == 0) {
            throw new NotFoundException("Cập nhật thất bại hoặc không có thay đổi.");
        }

        return command.id();
    }
}