package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSystemRubricVersionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricVersionDto;
import com.sep.vox.domain.mapper.RubricVersionDtoMapper;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViewSystemRubricVersionsUseCase implements IUseCase<ViewSystemRubricVersionsQuery, PageResult<RubricVersionDto>> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public ViewSystemRubricVersionsUseCase(
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RubricVersionDto> execute(ViewSystemRubricVersionsQuery query) {
        // 1. Xác thực User
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        // 2. Lấy Rubric gốc và kiểm tra quyền SYSTEM
        var rubric = rubricRepository.findById(query.rubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Hành động bị từ chối: Bộ Rubric này không thuộc về Hệ thống.");
        }

        // 3. Xử lý safeStatus
        String safeStatus = null;
        if (query.status() != null && !query.status().isBlank()) {
            try {
                // Ép kiểu thử sang Enum, nếu sai nó sẽ văng IllegalArgumentException
                safeStatus = RubricStatus.valueOf(query.status().trim().toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Trạng thái (status) không hợp lệ. Chỉ chấp nhận DRAFT, PUBLISHED, ARCHIVED.");
            }
        }

        // 3. Lấy danh sách Version phân trang từ Database (Hàm này đã có sẵn sort giảm dần ở bài trước)
        var versionPage = rubricVersionRepository.findAllByRubricIdAndStatus(
                query.rubricId(),
                safeStatus,
                query.page(),
                query.size()
        );

        // 4. Map sang DTO và trả về
        return RubricVersionDtoMapper.toRubricVersionPage(versionPage);
    }
}