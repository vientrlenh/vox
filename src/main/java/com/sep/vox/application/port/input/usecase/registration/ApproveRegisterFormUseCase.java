package com.sep.vox.application.port.input.usecase.registration;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.PasswordSetUpEmailRequestedEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ApproveRegisterFormCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;

@Service
public class ApproveRegisterFormUseCase implements IUseCase<ApproveRegisterFormCommand, Void> {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final RegisterFormRepository registerFormRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordSetUpTokenPort passwordSetUpTokenPort;
    private final PasswordSetUpTokenRepository passwordSetUpTokenRepository;
    private final UserContextPort userContextPort;
    private final EventPublisherPort eventPublisherPort;

    public ApproveRegisterFormUseCase(
            UserRepository userRepository,
            SchoolRepository schoolRepository,
            RegisterFormRepository registerFormRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordSetUpTokenPort passwordSetUpTokenPort,
            PasswordSetUpTokenRepository passwordSetUpTokenRepository,
            UserContextPort userContextPort, 
            EventPublisherPort eventPublisherPort) {
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.registerFormRepository = registerFormRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordSetUpTokenPort = passwordSetUpTokenPort;
        this.passwordSetUpTokenRepository = passwordSetUpTokenRepository;
        this.userContextPort = userContextPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    private static final String PASSWORD_NOT_SET = "__PASSWORD_NOT_SET__";

    @Override
    @Transactional
    public Void execute(ApproveRegisterFormCommand input) {
        var command = normalize(input);
        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var schoolAdminRole = roleRepository.findByCode("SCHOOL_ADMIN")
            .orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò quản trị nhà trường"));
        
        var updatedRows = registerFormRepository.updateApprovedRegisterForm(command.registerFormId(), currentUserId, now);
        if (updatedRows == 0) {
            throw new IllegalStateException("Đơn đăng ký không ở trạng thái chờ hoặc không tồn tại");
        }


        var savedSchool = saveSchool(command, currentUserId, now);
        var savedSchoolAdmin = saveSchoolAdmin(command, currentUserId, savedSchool.getId(), now);
        saveSchoolAdminUserRole(savedSchoolAdmin.getId(), schoolAdminRole.getId());

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

        return null;
    }

    private ApproveRegisterFormCommand normalize(ApproveRegisterFormCommand input) {
        return new ApproveRegisterFormCommand(
            input.registerFormId(),
            StringNormalization.normalizeCode(input.schoolCode()), 
            StringNormalization.trimAndCollapseSpaces(input.schoolName()), 
            StringNormalization.trimAndCollapseSpaces(input.description()), 
            StringNormalization.normalizePhone(input.contactPhone()), 
            StringNormalization.normalizeEmail(input.contactEmail()), 
            StringNormalization.normalizeDomain(input.schoolDomain()), 
            StringNormalization.trimAndCollapseSpaces(input.schoolAddress()), 
            input.studentCount(), 
            StringNormalization.trimAndCollapseSpaces(input.contactFullName()), 
            input.dateOfBirth(), 
            StringNormalization.trimAndCollapseSpaces(input.contactAddress())
        );
    }

    private School saveSchool(ApproveRegisterFormCommand command, UUID createdUserId, OffsetDateTime now) {
        var school = School.create(
            command.schoolCode(), 
            command.schoolName(), 
            command.description(), 
            command.contactPhone(), 
            command.contactEmail(), 
            command.schoolDomain(), 
            command.schoolAddress(), 
            command.studentCount(), 
            createdUserId,
            now
        );
        return schoolRepository.save(school);
    }

    private User saveSchoolAdmin(ApproveRegisterFormCommand command, UUID createdUserId, UUID schoolId, OffsetDateTime now) {
        var schoolAdmin = User.createSchoolAdmin(
            command.contactEmail(), 
            PASSWORD_NOT_SET, 
            command.contactPhone(), 
            command.contactFullName(), 
            command.dateOfBirth(), 
            command.contactAddress(), 
            null,
            createdUserId, 
            schoolId, 
            now
        );
        return userRepository.save(schoolAdmin);
    }

    private void saveSchoolAdminUserRole(UUID schoolAdminId, UUID schoolAdminRoleId) {
        var userRole = new UserRole(schoolAdminId, schoolAdminRoleId, OffsetDateTime.now());
        userRoleRepository.save(userRole);
    }

    
}
