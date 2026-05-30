package com.sep.vox.application.port.input.usecase.schooluser;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ChangeSchoolUserRoleCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;

@Service
public class ChangeSchoolUserRoleUseCase implements IUseCase<ChangeSchoolUserRoleCommand, Void> {

    private static final List<String> ALLOWED_ROLE_CODES = List.of("STUDENT", "TEACHER");

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public ChangeSchoolUserRoleUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    @Transactional
    public Void execute(ChangeSchoolUserRoleCommand input) {
        var callerId = userContextPort.getCurrentAuthenticatedUserId();

        var caller = userRepository.findById(callerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        if (!input.schoolId().equals(caller.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }

        var targetUser = userRepository.findById(input.userId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        if (!input.schoolId().equals(targetUser.getSchoolId())) {
            throw new NotFoundException("Không tìm thấy người dùng");
        }

        var newRoleCode = input.newRoleCode() != null ? input.newRoleCode().trim().toUpperCase() : null;
        if (!ALLOWED_ROLE_CODES.contains(newRoleCode)) {
            throw new IllegalArgumentException("Vai trò không hợp lệ, chỉ chấp nhận STUDENT hoặc TEACHER");
        }

        var newRole = roleRepository.findByCode(newRoleCode)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò: " + newRoleCode));

        var userRoles = userRoleRepository.findByUserId(input.userId());
        var schoolRoleRow = userRoles.stream()
            .filter(ur -> {
                var role = roleRepository.findById(ur.getRoleId());
                return role.map(r -> ALLOWED_ROLE_CODES.contains(r.getCode().value())).orElse(false);
            })
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò hiện tại của người dùng"));

        schoolRoleRow.setRoleId(newRole.getId());
        userRoleRepository.save(schoolRoleRow);

        return null;
    }
}
