package com.sep.vox.application.port.input.usecase.school;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateSchoolCommand;
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

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UpdateSchoolUseCase implements IUseCase<UpdateSchoolCommand, UUID> {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository; // Thêm Repo này
    private final UserContextPort userContextPort;

    public UpdateSchoolUseCase(
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
    public UUID execute(UpdateSchoolCommand command) {
        // 1. Validate sự tồn tại của trường học
        School school = schoolRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));

        // 2. Validate User bằng hàm exists cho tối ưu hiệu năng
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }

        // VÒNG BẢO MẬT: KIỂM TRA BẰNG SCHOOL USER
        var schoolUserOpt = schoolUserRepository.findByUserId(currentUserId);

        // Nếu user có liên kết với một trường học (tức là School Admin)
        if (schoolUserOpt.isPresent()) {
            SchoolUser schoolUser = schoolUserOpt.get();
            if (!schoolUser.getSchoolId().equals(school.getId())) {
                throw new ForbiddenException("BẢO MẬT: Bạn không có quyền sửa thông tin trường học của đơn vị khác.");
            }
        }


        // 3. Chuẩn hóa dữ liệu trước khi gửi xuống DB
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

        // 4. Thực thi Atomic Update
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