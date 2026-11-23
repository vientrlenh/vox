package com.sep.vox.application.port.input.usecase.schooluser;


import com.sep.vox.domain.model.school.SchoolUser;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.RoleConstant;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.mapper.SchoolUserDtoMapper;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ViewSchoolUsersBySchoolUseCase implements IUseCase<ViewSchoolUsersBySchoolQuery, PageResult<SchoolUserDto>> {

    private static final Set<String> ADMIN_ROLE_CODES =
        Set.of(RoleConstant.SCHOOL_ADMIN_ROLE, RoleConstant.SYSTEM_ADMIN_ROLE);

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolUserDto> execute(ViewSchoolUsersBySchoolQuery input) {
        UUID callerId = userContextPort.getCurrentAuthenticatedUserId();
        UUID schoolId = userContextPort.getCurrentSchoolId();

        if (schoolId == null || !input.schoolId().equals(schoolId)) {
            throw new ForbiddenException("Bạn không có quyền xem danh sách người dùng của trường này");
        }

        if (!userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Trạng thái người dùng không hợp lệ");
        }
        if (!schoolUserRepository.existsBySchoolIdAndUserId(input.schoolId(), callerId)) {
            throw new ForbiddenException("Quyền truy cập không hợp lệ");
        }

        String status = input.status() == null ? null : input.status().name();
        UUID roleId = resolveRoleId(input.roleId(), input.roleCode());

        PageResult<SchoolUser> schoolUsersPage = schoolUserRepository.findBySchoolId(
            input.schoolId(),
            StringNormalization.trimAndCollapseSpaces(input.search()),
            roleId,
            status,
            input.excludeClassId(),
            !isAdminRoleFilter(roleId),
            input.page(),
            input.size()
        );

        return SchoolUserDtoMapper.toSchoolUserPageDto(schoolUsersPage);
    }

    /**
     * The school user list always hides admin accounts (the screens that use it -- class assignment,
     * picking teachers/students -- do not act on administrators). They are returned only when the
     * client filters by an admin role.
     */
    private boolean isAdminRoleFilter(UUID roleId) {
        if (roleId == null) {
            return false;
        }
        return roleRepository.findById(roleId)
            .map(role -> ADMIN_ROLE_CODES.contains(role.getCode().value()))
            .orElse(false);
    }

    /**
     * Allows filtering by role code (STUDENT/TEACHER) because the school client cannot query the
     * role list to obtain the UUID. A {@code roleId} passed directly still takes priority when present.
     */
    private UUID resolveRoleId(UUID roleId, String roleCode) {
        if (roleId != null) {
            return roleId;
        }

        String normalized = StringNormalization.trimAndCollapseSpaces(roleCode);
        if (normalized == null) {
            return null;
        }

        return roleRepository.findByCode(normalized.toUpperCase())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò"))
            .getId();
    }
}
