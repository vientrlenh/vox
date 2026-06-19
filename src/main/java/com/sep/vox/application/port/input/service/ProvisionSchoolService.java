package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.event.PasswordSetUpEmailRequestedEvent;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ProvisionSchoolCommand;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
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
    private final PasswordSetUpTokenRepository passwordSetUpTokenRepository;
    private final PasswordSetUpTokenPort passwordSetUpTokenPort;
    private final EventPublisherPort eventPublisherPort;

    public ProvisionSchoolService(
        UserRepository userRepository, 
        RoleRepository roleRepository, 
        SchoolRepository schoolRepository, 
        SchoolUserRepository schoolUserRepository, 
        UserRoleRepository userRoleRepository,
        PasswordSetUpTokenRepository passwordSetUpTokenRepository, 
        PasswordSetUpTokenPort passwordSetUpTokenPort, 
        EventPublisherPort eventPublisherPort
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.schoolRepository = schoolRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordSetUpTokenRepository = passwordSetUpTokenRepository;
        this.passwordSetUpTokenPort = passwordSetUpTokenPort;
        this.eventPublisherPort = eventPublisherPort;
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

        var passwordToken = passwordSetUpTokenPort.generateToken();
        var passwordSetUpToken = PasswordSetUpToken.create(savedSchoolAdmin.getId(), passwordToken.hashedToken());
        passwordSetUpTokenRepository.save(passwordSetUpToken);

        eventPublisherPort.publish(new PasswordSetUpEmailRequestedEvent(
            command.contactEmail(),
            command.contactFullName(),
            command.schoolName(),
            savedSchoolAdmin.getId(),
            passwordToken.rawToken()
        ));
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
