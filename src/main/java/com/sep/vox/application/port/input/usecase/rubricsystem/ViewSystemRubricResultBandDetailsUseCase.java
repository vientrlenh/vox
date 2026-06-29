package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSystemRubricResultBandDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.RubricResultBandDto;
import com.sep.vox.domain.mapper.RubricResultBandDtoMapper;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ViewSystemRubricResultBandDetailsUseCase implements IUseCase<ViewSystemRubricResultBandDetailsQuery, RubricResultBandDto> {

    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public ViewSystemRubricResultBandDetailsUseCase(
            RubricResultBandRepository rubricResultBandRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public RubricResultBandDto execute(ViewSystemRubricResultBandDetailsQuery query) {
        // 1. Xác thực tài khoản
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        // 2. Lấy Result Band
        var resultBand = rubricResultBandRepository.findById(query.resultBandId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Dải điểm Kết quả (Result Band) này."));

        // 3. Lấy Version
        var version = rubricVersionRepository.findById(resultBand.getRubricVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric Version chứa Dải điểm Kết quả này."));

        // 4. Lấy Rubric gốc và kiểm tra quyền sở hữu SYSTEM
        var rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Hành động bị từ chối: Dải điểm Kết quả này thuộc về Trường học, không phải của Hệ thống.");
        }

        // 5. Map sang DTO và trả về
        return RubricResultBandDtoMapper.toDto(resultBand);
    }
}