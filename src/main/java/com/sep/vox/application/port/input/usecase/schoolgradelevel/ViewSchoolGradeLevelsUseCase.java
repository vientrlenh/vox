package com.sep.vox.application.port.input.usecase.schoolgradelevel;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolGradeLevelsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolGradeLevelDto;
import com.sep.vox.domain.mapper.SchoolGradeLevelDtoMapper;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewSchoolGradeLevelsUseCase implements IUseCase<ViewSchoolGradeLevelsQuery, PageResult<SchoolGradeLevelDto>> {

    private final SchoolGradeLevelRepository schoolGradeLevelRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public ViewSchoolGradeLevelsUseCase(
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolGradeLevelDto> execute(ViewSchoolGradeLevelsQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        validateUserAndAccess(currentUserId, input.schoolId());

        if (!schoolRepository.existsById(input.schoolId())) {
            throw new NotFoundException("Không tìm thấy trường học.");
        }

        // Mặc định ẩn Khối đã xóa mềm: khi không lọc theo trạng thái thì chỉ trả về ACTIVE.
        // Admin vẫn xem được Khối đã xóa bằng cách truyền status=INACTIVE.
        var status = parseStatus(input.status());
        var effectiveStatus = status == null ? SchoolGradeLevelStatus.ACTIVE : status;

        var result = schoolGradeLevelRepository.findBySchoolId(
            input.schoolId(),
            StringNormalization.trimAndCollapseSpaces(input.search()),
            effectiveStatus,
            input.page(),
            input.size()
        );
        return SchoolGradeLevelDtoMapper.toDtoPage(result);
    }

    private void validateUserAndAccess(UUID userId, UUID targetSchoolId) {
        if (!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }
        if (!userContextPort.isSystemAdmin()) {
            UUID userSchoolId = schoolUserRepository.findSchoolIdByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền xem khối học của trường khác."));
            if (!userSchoolId.equals(targetSchoolId)) {
                throw new ForbiddenException("Bạn không có quyền xem khối học của trường khác.");
            }
        }
    }

    private SchoolGradeLevelStatus parseStatus(String status) {
        var normalized = StringNormalization.trimAndCollapseSpaces(status);
        if (normalized == null) {
            return null;
        }
        try {
            return SchoolGradeLevelStatus.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái khối học không hợp lệ");
        }
    }
}
