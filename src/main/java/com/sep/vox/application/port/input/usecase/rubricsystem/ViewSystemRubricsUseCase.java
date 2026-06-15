package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSystemRubricsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricDto;
import com.sep.vox.domain.mapper.RubricDtoMapper;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ViewSystemRubricsUseCase implements IUseCase<ViewSystemRubricsQuery, PageResult<RubricDto>> {

    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public ViewSystemRubricsUseCase(
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RubricDto> execute(ViewSystemRubricsQuery query) {
        // 1. Xác thực tài khoản (Đang Active)
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();


        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        // 2. Lấy dữ liệu phân trang từ Database (Chỉ lấy SYSTEM)
        var rubricPage = rubricRepository.findAllByOwnerType(
                RubricOwnerType.SYSTEM,
                query.page(),
                query.size()
        );

        // 3. Sử dụng Mapper đã viết sẵn để chuyển PageResult<Rubric> thành PageResult<RubricDto>
        return RubricDtoMapper.toRubricPage(rubricPage);
    }
}