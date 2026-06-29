package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSystemRubricCriterionBandDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.RubricCriterionBandDto;
import com.sep.vox.domain.mapper.RubricCriterionBandDtoMapper;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricCriterionBandRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ViewSystemRubricCriterionBandDetailsUseCase implements IUseCase<ViewSystemRubricCriterionBandDetailsQuery, RubricCriterionBandDto> {

    private final RubricCriterionBandRepository rubricCriterionBandRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public ViewSystemRubricCriterionBandDetailsUseCase(
            RubricCriterionBandRepository rubricCriterionBandRepository,
            RubricCriterionRepository rubricCriterionRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricCriterionBandRepository = rubricCriterionBandRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public RubricCriterionBandDto execute(ViewSystemRubricCriterionBandDetailsQuery query) {
        // 1. Xác thực tài khoản
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        // 2. Lấy Band
        var band = rubricCriterionBandRepository.findById(query.bandId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Dải điểm (Band) này."));

        // 3. Lấy Criterion
        var criterion = rubricCriterionRepository.findById(band.getCriterionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Tiêu chí chứa Dải điểm này."));

        // 4. Lấy Version
        var version = rubricVersionRepository.findById(criterion.getRubricVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric Version."));

        // 5. Lấy Rubric gốc và kiểm tra quyền sở hữu SYSTEM
        var rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Hành động bị từ chối: Dải điểm này thuộc về Trường học, không phải của Hệ thống.");
        }

        // 6. Map sang DTO và trả về
        return RubricCriterionBandDtoMapper.toDto(band);
    }
}