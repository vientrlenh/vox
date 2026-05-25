package com.sep.vox.application.port.input.usecase.systemadmin;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RegisterSchoolAdminCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.model.userrole.UserRole;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

@Service
public class RegisterSchoolAdminUseCase implements IUseCase<RegisterSchoolAdminCommand, Void> {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;

    public RegisterSchoolAdminUseCase(UserRepository userRepository, RoleRepository roleRepository, UserRoleRepository userRoleRepository, SchoolRepository schoolRepository, UserContextPort userContextPort) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.schoolRepository = schoolRepository;
        this.userContextPort = userContextPort;
    }

    private static final String PASSWORD_NOT_SET = "__PASSWORD_NOT_SET__";

    @Override
    @Transactional
    public Void execute(RegisterSchoolAdminCommand input) {
        var command = normalize(input);

        if (!schoolRepository.existsById(command.schoolId())) {
            throw new NotFoundException("Không tìm thấy trường yêu cầu");
        }
        var schoolAdminRole = roleRepository.findByCode("SCHOOL_ADMIN")
            .orElseThrow(() -> new NotFoundException("Không tìm thấy role quản trị nhà trường trong cơ sở dữ liệu"));
        var requestedUserId = userContextPort.getCurrentAuthenticatedUserId();
        var now = OffsetDateTime.now();
        var schoolAdminUser = new User(
            new Email(command.email()), 
            PASSWORD_NOT_SET, 
            new Phone(command.phone()), 
            new FullName(command.fullName()), 
            null, 
            new DateOfBirth(command.dateOfBirth()), 
            command.address(), 
            UserStatus.INACTIVE, 
            now, 
            now, 
            requestedUserId, 
            requestedUserId, 
            command.schoolId()
        );
        var savedUser = userRepository.save(schoolAdminUser);
        var userRole = new UserRole(
            savedUser.getId(), 
            schoolAdminRole.getId(), 
            OffsetDateTime.now()
        );
        userRoleRepository.save(userRole);
        return null;
    }

    private RegisterSchoolAdminCommand normalize(RegisterSchoolAdminCommand input) {
        return new RegisterSchoolAdminCommand(
            StringNormalization.normalizeEmail(input.email()), 
            StringNormalization.normalizePhone(input.phone()), 
            StringNormalization.trimAndCollapseSpaces(input.fullName()), 
            input.dateOfBirth(), 
            StringNormalization.trimAndCollapseSpaces(input.address()), 
            input.schoolId()
        );
    }

    
}
