package com.sep.vox.application.port.input.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.event.UserCreatedPayloadV1;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ProvisionSchoolCommand;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.common.UserTypeConstant;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;

@Service
public class ProvisionSchoolService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public ProvisionSchoolService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        SchoolRepository schoolRepository,
        SchoolUserRepository schoolUserRepository,
        UserRoleRepository userRoleRepository,
        OutboxRepository outboxRepository,
        JsonSerializationPort jsonSerializationPort
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.schoolRepository = schoolRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleRepository = userRoleRepository;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    public void provision(ProvisionSchoolCommand command) {
        var schoolAdminRole = roleRepository.findByCode("SCHOOL_ADMIN")
            .orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò quản trị nhà trường"));

        if (userRepository.existsByEmail(command.contactEmail())) {
            throw new DuplicatedException("Người dùng với email này đã tồn tại");
        }

        if (userRepository.existsByPhone(command.contactPhone())) {
            throw new DuplicatedException("Người dùng với số điện thoại này đã tồn tại");
        }

        if (schoolRepository.existsByCode(command.schoolCode())) {
            throw new DuplicatedException("Mã trường đã tồn tại");
        }

        if (command.schoolDomain() != null && schoolRepository.existsByDomain(command.schoolDomain())) {
            throw new DuplicatedException("Domain yêu cầu đã tồn tại trong hệ thống");
        }

        var savedSchool = saveSchool(command);
        var savedSchoolAdmin = saveSchoolAdmin(command,  savedSchool.getId());
        saveSchoolAdminUserRole(command, savedSchoolAdmin.getId(), schoolAdminRole.getId());

        var schoolUser = SchoolUser.create(savedSchoolAdmin.getId(), savedSchool.getId(), command.now(), null);
        schoolUserRepository.save(schoolUser);

        // Token đặt mật khẩu do consumer sinh ngay trước khi gửi mail, để nó không nằm
        // plaintext trong bảng outboxes lẫn trong topic Kafka.
        var event = new UserCreatedPayloadV1(
            savedSchoolAdmin.getId(),
            command.contactEmail(),
            command.contactFullName(),
            command.schoolName(),
            UserTypeConstant.SCHOOL_ADMIN
        );
        var payload = jsonSerializationPort.toJson(event);
        var outbox = Outbox.create(
            AggregateTypeConstant.USER, savedSchoolAdmin.getId(), EventTypeConstant.USER_CREATED, payload, OffsetDateTime.now());
        outboxRepository.save(outbox);
    }

    private School saveSchool(ProvisionSchoolCommand command) {
        var school = School.create(
            command.schoolCode(), 
            command.schoolName(), 
            command.description(), 
            command.contactPhone(), 
            command.contactEmail(), 
            command.schoolDomain(), 
            command.schoolAddress(), 
            command.studentCount(), 
            command.createdUserId(),
            command.now()
        );
        return schoolRepository.save(school);
    }

    private User saveSchoolAdmin(ProvisionSchoolCommand command, UUID schoolId) {
        var schoolAdmin = User.createSchoolAdmin(
            command.contactEmail(),  
            command.contactPhone(), 
            command.contactFullName(), 
            command.dateOfBirth(), 
            command.contactAddress(), 
            command.avatarUrl(),
            command.createdUserId(), 
            command.now()
        );
        return userRepository.save(schoolAdmin);
    }

    private void saveSchoolAdminUserRole(ProvisionSchoolCommand command, UUID schoolAdminId, UUID schoolAdminRoleId) {
        var userRole = new UserRole(schoolAdminId, schoolAdminRoleId, command.now());
        userRoleRepository.save(userRole);
    }
}
