package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.*;
import com.sep.vox.application.port.input.query.rubricsystem.SearchSystemRubricResultBandsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricResultBandDto;
import com.sep.vox.domain.mapper.RubricResultBandDtoMapper;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchSystemRubricResultBandsUseCase implements IUseCase<SearchSystemRubricResultBandsQuery, PageResult<RubricResultBandDto>> {

    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<RubricResultBandDto> execute(SearchSystemRubricResultBandsQuery query) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("The account does not exist."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("The account has been locked.");

        RubricVersion version = rubricVersionRepository.findById(query.versionId()).orElseThrow(() -> new ResourceNotFoundException("Rubric version not found."));
        Rubric rubric = rubricRepository.findById(version.getRubricId()).orElseThrow(() -> new ResourceNotFoundException("Parent Rubric not found."));

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("SECURITY: this data belongs to a School. You are not allowed to access it.");
        }

        PageResult<RubricResultBand> pageResult = rubricResultBandRepository.searchRubricResultBands(query.versionId(), query.keyword(), query.page(), query.size());

        return new PageResult<>(
                pageResult.content().stream()
                        .map(RubricResultBandDtoMapper::toDto)
                        .toList(),
                pageResult.page(), pageResult.size(), pageResult.totalElements(), pageResult.totalPages()
        );
    }
}