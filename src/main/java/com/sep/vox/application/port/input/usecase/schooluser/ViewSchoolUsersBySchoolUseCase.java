package com.sep.vox.application.port.input.usecase.schooluser;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.mapper.SchoolUserDtoMapper;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

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
        var callerId = userContextPort.getCurrentAuthenticatedUserId();

        if (!userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Trạng thái người dùng không hợp lệ");
        }
        if (!schoolUserRepository.existsBySchoolIdAndUserId(input.schoolId(), callerId)) {
            throw new ForbiddenException("Quyền truy cập không hợp lệ");
        }

        var role = validateRole(input.role());
        var status = validateStatus(input.status());

        var schoolUsersPage = schoolUserRepository.findBySchoolId(
            input.schoolId(),
            StringNormalization.trimAndCollapseSpaces(input.search()),
            role,
            status,
            SchoolRoleCodes.ALL,
            input.page(),
            input.size()
        );

        return SchoolUserDtoMapper.toSchoolUserPageDto(schoolUsersPage);
    }

    private String validateRole(String role) {
        var normalized = StringNormalization.trimAndCollapseSpaces(role);
        if (normalized == null) {
            return null;
        }
        var upper = normalized.toUpperCase();
        if (!SchoolRoleCodes.ALL.contains(upper)) {
            throw new IllegalArgumentException("Vai trò không hợp lệ, chỉ chấp nhận STUDENT hoặc TEACHER");
        }
        return upper;
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
