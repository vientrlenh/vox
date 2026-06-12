package com.sep.vox.application.port.input.usecase.schooluser;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.UserStatusValidator;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ChangeSchoolUserRoleCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.domain.model.user.UserRole;

@Service
public class ChangeSchoolUserRoleUseCase implements IUseCase<ChangeSchoolUserRoleCommand, Void> {

    private static final List<String> ALLOWED_ROLE_CODES = List.of("STUDENT", "TEACHER");

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SchoolUserRepository schoolUserRepository;

    public ChangeSchoolUserRoleUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            SchoolUserRepository schoolUserRepository) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public Void execute(ChangeSchoolUserRoleCommand input) {
        var callerId = userContextPort.getCurrentAuthenticatedUserId();

        var caller = userRepository.findById(callerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        UserStatusValidator.requireActive(caller);
        var callerSchoolUser = schoolUserRepository.findByUserId(callerId)
            .orElseThrow(() -> new IllegalArgumentException("Không có quyền thực hiện thao tác này"));
        if (!input.schoolId().equals(callerSchoolUser.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }

        var targetUser = userRepository.findById(input.userId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        UserStatusValidator.requireActiveTarget(targetUser);
        var targetSchoolUser = schoolUserRepository.findByUserId(input.userId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        if (!input.schoolId().equals(targetSchoolUser.getSchoolId())) {
            throw new NotFoundException("Không tìm thấy người dùng");
        }

        var newRoleCode = input.newRoleCode() != null ? input.newRoleCode().trim().toUpperCase() : null;
        if (!ALLOWED_ROLE_CODES.contains(newRoleCode)) {
            throw new IllegalArgumentException("Vai trò không hợp lệ, chỉ chấp nhận STUDENT hoặc TEACHER");
        }

        var userRoles = userRoleRepository.findByUserId(input.userId());
        UserRole schoolRoleRow = null;
        String currentRoleCode = null;
        for (var userRole : userRoles) {
            var role = roleRepository.findById(userRole.getRoleId()).orElse(null);
            if (role != null && ALLOWED_ROLE_CODES.contains(role.getCode().value())) {
                schoolRoleRow = userRole;
                currentRoleCode = role.getCode().value();
                break;
            }
        }

        if (schoolRoleRow == null) {
            throw new NotFoundException("Không tìm thấy vai trò hiện tại của người dùng");
        }

        if (Objects.equals(currentRoleCode, newRoleCode)) {
            return null;
        }

        var newRole = roleRepository.findByCode(newRoleCode)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò: " + newRoleCode));

        var updated = userRoleRepository.compareAndSetRoleId(
            schoolRoleRow.getId(), schoolRoleRow.getRoleId(), newRole.getId());
        if (updated == 0) {
            throw new IllegalStateException("Vai trò của người dùng vừa được thay đổi bởi thao tác khác, vui lòng thử lại");
        }


        return null;
    }
}
