package com.sep.vox.application.port.input.usecase.schooluser;


import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.model.school.SchoolUserRole;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.shared.PageResult;

@Service
public class ViewSchoolUsersBySchoolUseCase implements IUseCase<ViewSchoolUsersBySchoolQuery, PageResult<SchoolUserDto>> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;

    public ViewSchoolUsersBySchoolUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolUserDto> execute(ViewSchoolUsersBySchoolQuery input) {
        UUID callerId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        UUID schoolId = userContextPort.getCurrentSchoolId().orElse(null);

        if (schoolId == null || !input.schoolId().equals(schoolId)) {
            throw new ForbiddenException("Bạn không có quyền xem danh sách người dùng của trường này");
        }

        if (!userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Trạng thái người dùng không hợp lệ");
        }
        if (!schoolUserRepository.existsBySchoolIdAndUserId(input.schoolId(), callerId)) {
            throw new ForbiddenException("Quyền truy cập không hợp lệ");
        }

        var status = validateStatus(input.status());

        var schoolUsersPage = schoolUserRepository.findBySchoolId(
            input.schoolId(),
            StringNormalization.trimAndCollapseSpaces(input.search()),
            input.role(),
            status,
            input.excludeClassId(),
            !isAdminRoleFilter(input.role()),
            input.pageRequest().repositoryPage(),
            input.pageRequest().size()
        );

        return SchoolUserDto.toDtoPage(schoolUsersPage);
    }

    /**
     * Danh sách người dùng của trường luôn ẩn tài khoản quản trị (các màn hình dùng nó — xếp lớp,
     * chọn giáo viên/học sinh — không thao tác trên quản trị viên). Chỉ khi client lọc đúng vai trò
     * quản trị thì mới trả về họ.
     */
    private boolean isAdminRoleFilter(SchoolUserRole role) {
        return role == SchoolUserRole.SCHOOL_ADMIN;
    }

    private String validateStatus(String status) {
        var normalized = StringNormalization.trimAndCollapseSpaces(status);
        if (normalized == null) {
            return null;
        }
        try {
            return UserStatus.valueOf(normalized.toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái người dùng không hợp lệ");
        }
    }
}
