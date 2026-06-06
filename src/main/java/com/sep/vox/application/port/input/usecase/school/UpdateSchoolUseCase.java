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
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.SchoolDomain;
import com.sep.vox.domain.valueobject.StudentCount;
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
        // 1. Lấy thông tin trường
        School school = schoolRepository.findById(command.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học với ID đã cho."));

        // 2. Lấy User và Check quyền System Admin / School Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản của bạn."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        // Cảnh báo: Chỉ Admin của đúng trường đó mới được sửa. System Admin có quyền sửa mọi trường.
        if (currentUser.getSchoolId() != null && !currentUser.getSchoolId().equals(school.getId())) {
            throw new UnauthorizedException("CẢNH BÁO BẢO MẬT: Bạn không có quyền cập nhật thông tin của trường học khác!");
        }

        // 3. CẬP NHẬT TỪNG PHẦN (PARTIAL UPDATE)
        if (command.name() != null) {
            school.setName(StringNormalization.trimAndCollapseSpaces(command.name()));
        }

        if (command.description() != null) {
            school.setDescription(StringNormalization.trimAndCollapseSpaces(command.description()));
        }


        if (command.contactPhone() != null) {
            String normalizedPhone = StringNormalization.normalizePhone(command.contactPhone());
            if (schoolRepository.existsByContactPhoneAndIdNot(normalizedPhone, command.id())) {
                throw new DuplicatedException("Số điện thoại này đã được sử dụng bởi trường khác.");
            }
            school.setContactPhone(new Phone(normalizedPhone));
        }

        if (command.contactEmail() != null) {
            String normalizedEmail = StringNormalization.normalizeEmail(command.contactEmail());
            if (schoolRepository.existsByContactEmailAndIdNot(normalizedEmail, command.id())) {
                throw new DuplicatedException("Email này đã được sử dụng bởi trường khác.");
            }
            school.setContactEmail(new Email(normalizedEmail));
        }

        if (command.domain() != null) {
            String normalizedDomain = StringNormalization.normalizeDomain(command.domain());
            if (schoolRepository.existsByDomainAndIdNot(normalizedDomain, command.id())) {
                throw new DuplicatedException("Tên miền này đã được sử dụng bởi trường khác.");
            }
            school.setDomain(new SchoolDomain(normalizedDomain));
        }

        if (command.address() != null) {
            school.setAddress(StringNormalization.trimAndCollapseSpaces(command.address()));
        }

        if (command.studentCount() != null) {
            school.setStudentCount(new StudentCount(command.studentCount()));
        }

        // 4. Cập nhật người sửa cuối và lưu xuống DB
        school.setUpdatedBy(currentUserId);
        school.setUpdatedAt(OffsetDateTime.now());

        School updatedSchool = schoolRepository.save(school);

        return updatedSchool.getId();
    }
}