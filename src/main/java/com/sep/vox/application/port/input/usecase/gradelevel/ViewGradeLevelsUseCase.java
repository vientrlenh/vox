package com.sep.vox.application.port.input.usecase.gradelevel;

import com.sep.vox.domain.model.gradelevel.GradeLevel;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.gradelevel.ViewGradeLevelsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.GradeLevelDto;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Reads the grade level catalog: every active user can view it (no longer limited by school,
 * because the data is shared). Only write operations are limited to the system admin.
 */
@Service
@RequiredArgsConstructor
public class ViewGradeLevelsUseCase implements IUseCase<ViewGradeLevelsQuery, PageResult<GradeLevelDto>> {

    private final GradeLevelRepository gradeLevelRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public PageResult<GradeLevelDto> execute(ViewGradeLevelsQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The account does not exist or has been locked.");
        }

        // Soft-deleted grade levels are hidden by default: without a status filter only ACTIVE is returned.
        // Admins can still see deleted grade levels by passing status=INACTIVE.
        GradeLevelStatus status = input.status();
        GradeLevelStatus effectiveStatus = status == null ? GradeLevelStatus.ACTIVE : status;

        PageResult<GradeLevel> result = gradeLevelRepository.findAll(
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
}
