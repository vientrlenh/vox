package com.sep.vox.application.port.input.usecase.schooluser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.mapper.SchoolUserDtoMapper;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Phục vụ query `schoolUsersBySchool` cho cả SCHOOL_ADMIN lẫn TEACHER (picker tìm đồng nghiệp ở Question
 * Share / Exam Member). Tách hoàn toàn khỏi {@link ViewSchoolUsersBySchoolUseCase} (không gọi/inject use
 * case đó) để 2 nhánh không ràng buộc lẫn nhau: SCHOOL_ADMIN/SYSTEM_ADMIN được lọc theo `role` truyền vào
 * như cũ; TEACHER luôn bị ép `role=TEACHER` ở tầng này, không phụ thuộc input, nên không thể duyệt danh
 * sách học sinh trong trường.
 */
@Service
public class ViewSchoolUsersForRequesterUseCase implements IUseCase<ViewSchoolUsersBySchoolQuery, PageResult<SchoolUserDto>> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public ViewSchoolUsersForRequesterUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
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

        var isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(callerId).stream()
            .anyMatch(r -> "SYSTEM_ADMIN".equals(r.roleCode()) || "SCHOOL_ADMIN".equals(r.roleCode()));

        var role = isSchoolAdmin ? validateRole(input.role()) : "TEACHER";
        var status = validateStatus(input.status());
        // TEACHER dùng query này như picker tìm đồng nghiệp (chia sẻ câu hỏi / gán thành viên exam) nên không
        // cần thấy chính mình trong kết quả; SCHOOL_ADMIN/SYSTEM_ADMIN vẫn xem được danh sách đầy đủ như cũ.
        var excludeUserId = isSchoolAdmin ? null : callerId;

        var schoolUsersPage = schoolUserRepository.findBySchoolId(
            input.schoolId(),
            excludeUserId,
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
