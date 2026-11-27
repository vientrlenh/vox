package com.sep.vox.application.port.input.usecase.schooluser;

import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.domain.model.school.SchoolUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.schooluser.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.mapper.SchoolUserDtoMapper;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

import java.util.UUID;

/**
 * Serves the `schoolUsersBySchool` query for both SCHOOL_ADMIN and TEACHER (the colleague-search picker in Question
 * Share / Assessment Member). Fully separated from {@link ViewSchoolUsersBySchoolUseCase} (does not call/inject that use
 * case) so the 2 branches do not constrain each other: SCHOOL_ADMIN/SYSTEM_ADMIN are filtered by the passed-in `role`
 * as before; TEACHER is always forced to `role=TEACHER` at this layer, independent of the input, so they cannot browse the
 * list of students in the school.
 */
@Service
@RequiredArgsConstructor
public class ViewSchoolUsersForRequesterUseCase implements IUseCase<ViewSchoolUsersBySchoolQuery, PageResult<SchoolUserDto>> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolUserDto> execute(ViewSchoolUsersBySchoolQuery input) {
        UUID callerId = userContextPort.getCurrentAuthenticatedUserId();

        requireCallerBelongsToSchool(callerId, input.schoolId());

        List<UserRoleInfo> callerRoles = userRoleQueryRepository.findByUserIdWithRoleInfo(callerId);
        boolean isSchoolAdmin = callerRoles.stream()
            .anyMatch(r -> "SYSTEM_ADMIN".equals(r.roleCode()) || "SCHOOL_ADMIN".equals(r.roleCode()));

        RoleFilter roleFilter = resolveRoleFilter(isSchoolAdmin, input, callerRoles, callerId);

        String status = input.status() == null ? null : input.status().name();

        PageResult<SchoolUser> schoolUsersPage = schoolUserRepository.searchBySchoolId(
            input.schoolId(),
            roleFilter.excludeUserId(),
            StringNormalization.trimAndCollapseSpaces(input.search()),
            roleFilter.roleId(),
            status,
            input.page(),
            input.size()
        );

        return SchoolUserDtoMapper.toSchoolUserPageDto(schoolUsersPage);
    }

    private void requireCallerBelongsToSchool(UUID callerId, UUID schoolId) {
        if (!userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Trạng thái người dùng không hợp lệ");
        }
        if (!schoolUserRepository.existsBySchoolIdAndUserId(schoolId, callerId)) {
            throw new ForbiddenException("Quyền truy cập không hợp lệ");
        }
    }

    // Admin: filter by whatever role the caller passes (null = all); sees everyone.
    // TEACHER: force filter to TEACHER role UUID, exclude self from results.
    private RoleFilter resolveRoleFilter(
            boolean isSchoolAdmin, ViewSchoolUsersBySchoolQuery input, List<UserRoleInfo> callerRoles, UUID callerId) {
        if (isSchoolAdmin) {
            return new RoleFilter(input.roleId(), null);
        }
        // A TEACHER must hold the TEACHER role; otherwise reject instead of leaving roleId=null
        // (roleId=null would make the query return every role and expose the student list).
        UUID teacherRoleId = callerRoles.stream()
            .filter(r -> "TEACHER".equals(r.roleCode()))
            .map(r -> r.roleId())
            .findFirst()
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập không hợp lệ"));
        return new RoleFilter(teacherRoleId, callerId);
    }

    private record RoleFilter(UUID roleId, UUID excludeUserId) {
    }
}
