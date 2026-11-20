package com.sep.vox.application.port.input.usecase.gradelevel;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewGradeLevelsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.GradeLevelDto;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Đọc catalog khối lớp: mọi user đang hoạt động đều xem được (không còn giới hạn theo trường,
 * vì dữ liệu đã dùng chung). Chỉ thao tác ghi mới giới hạn system admin.
 */
@Service
public class ViewGradeLevelsUseCase implements IUseCase<ViewGradeLevelsQuery, PageResult<GradeLevelDto>> {

    private final GradeLevelRepository gradeLevelRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public ViewGradeLevelsUseCase(
            GradeLevelRepository gradeLevelRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.gradeLevelRepository = gradeLevelRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<GradeLevelDto> execute(ViewGradeLevelsQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }

        // Mặc định ẩn Khối đã xóa mềm: khi không lọc theo trạng thái thì chỉ trả về ACTIVE.
        // Admin vẫn xem được Khối đã xóa bằng cách truyền status=INACTIVE.
        var status = parseStatus(input.status());
        var effectiveStatus = status == null ? GradeLevelStatus.ACTIVE : status;

        var result = gradeLevelRepository.findAll(
            StringNormalization.trimAndCollapseSpaces(input.search()),
            effectiveStatus,
            input.page(),
            input.size()
        );
        return new PageResult<>(
            result.content().stream().map(GradeLevelDto::toDto).toList(),
            result.page(),
            result.size(),
            result.totalElements(),
            result.totalPages()
        );
    }

    private GradeLevelStatus parseStatus(String status) {
        var normalized = StringNormalization.trimAndCollapseSpaces(status);
        if (normalized == null) {
            return null;
        }
        try {
            return GradeLevelStatus.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái khối học không hợp lệ");
        }
    }
}
