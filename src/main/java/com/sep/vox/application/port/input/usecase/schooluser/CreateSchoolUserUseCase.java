package com.sep.vox.application.port.input.usecase.schooluser;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.schooluser.SchoolUserResponseMapper;
import com.sep.vox.application.port.input.command.CreateSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.domain.model.schooluser.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.userrole.UserRole;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;

@Service
public class CreateSchoolUserUseCase implements IUseCase<CreateSchoolUserCommand, SchoolUserResponse> {

    private static final List<String> ALLOWED_ROLE_CODES = List.of("STUDENT", "TEACHER");

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public CreateSchoolUserUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @Override
    @Transactional
    public SchoolUserResponse execute(CreateSchoolUserCommand input) {
        var now = OffsetDateTime.now();
        var callerId = userContextPort.getCurrentAuthenticatedUserId();

        var caller = userRepository.findById(callerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        SchoolUserStatusValidator.requireActive(caller);
        if (!input.schoolId().equals(caller.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }

        var command = normalize(input);

        if (!ALLOWED_ROLE_CODES.contains(command.roleCode())) {
            throw new IllegalArgumentException("Vai trò không hợp lệ, chỉ chấp nhận STUDENT hoặc TEACHER");
        }
        var role = roleRepository.findByCode(command.roleCode())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò: " + command.roleCode()));

        if (userRepository.findByEmail(command.email()).isPresent()) {
            throw new DuplicatedException("Email đã tồn tại");
        }
        if (userRepository.findByPhone(command.phone()).isPresent()) {
            throw new DuplicatedException("Số điện thoại đã tồn tại");
        }

        User user = command.roleCode().equals("STUDENT")
            ? User.createStudent(command.email(), command.phone(), command.fullName(), command.dateOfBirth(), command.address(), callerId, command.schoolId(), now)
            : User.createTeacher(command.email(), command.phone(), command.fullName(), command.dateOfBirth(), command.address(), callerId, command.schoolId(), now);

        var savedUser = userRepository.save(user);

        userRoleRepository.save(new UserRole(savedUser.getId(), role.getId(), now));

        SchoolUser savedSchoolUser = null;
        if ("STUDENT".equals(command.roleCode()) && command.studentId() != null) {
            savedSchoolUser = schoolUserRepository.save(
                SchoolUser.create(savedUser.getId(), command.schoolId(), command.studentId(), callerId, now)
            );
        }

        var roleInfo = userRoleQueryRepository.findByUserIdWithRoleInfo(savedUser.getId());
        var resolvedRoleCode = roleInfo.isEmpty() ? command.roleCode() : roleInfo.get(0).roleCode();

        return SchoolUserResponseMapper.toResponse(savedUser, resolvedRoleCode, savedSchoolUser);
    }

    private CreateSchoolUserCommand normalize(CreateSchoolUserCommand input) {
        return new CreateSchoolUserCommand(
            input.schoolId(),
            StringNormalization.normalizeEmail(input.email()),
            StringNormalization.normalizePhone(input.phone()),
            StringNormalization.trimAndCollapseSpaces(input.fullName()),
            input.dateOfBirth(),
            input.address() != null ? StringNormalization.trimAndCollapseSpaces(input.address()) : null,
            input.roleCode() != null ? input.roleCode().trim().toUpperCase() : null,
            input.studentId() != null ? input.studentId().trim() : null
        );
    }
}
