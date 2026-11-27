package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.rubricsystem.ViewSystemRubricResultBandsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricResultBandDto;
import com.sep.vox.domain.mapper.RubricResultBandDtoMapper;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewSystemRubricResultBandsUseCase implements IUseCase<ViewSystemRubricResultBandsQuery, PageResult<RubricResultBandDto>> {

    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public PageResult<RubricResultBandDto> execute(ViewSystemRubricResultBandsQuery query) {
        requireActiveCurrentUser();
        requireSystemOwnedVersion(query.versionId());

        // The database already sorts ascending by order.
        PageResult<RubricResultBand> resultBandsPage = rubricResultBandRepository.findAllByRubricVersionId(
                query.versionId(),
                query.page(),
                query.size()
        );

        return RubricResultBandDtoMapper.toPage(resultBandsPage);
    }

    private void requireActiveCurrentUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The account does not exist or is locked.");
        }
    }

    private void requireSystemOwnedVersion(UUID versionId) {
        RubricVersion version = rubricVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Rubric Version not found."));

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Rubric not found."));

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Action denied: this version belongs to a School, not to the System.");
        }
    }
}
