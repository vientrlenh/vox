package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.rubricsystem.SearchSystemRubricVersionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricVersionDto;
import com.sep.vox.domain.mapper.RubricVersionDtoMapper;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchSystemRubricVersionsUseCase implements IUseCase<SearchSystemRubricVersionsQuery, PageResult<RubricVersionDto>> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<RubricVersionDto> execute(SearchSystemRubricVersionsQuery query) {
        // 1. Authenticate the System Admin account
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        // 2. System security checkpoint: prevent cross-viewing school Rubrics
        Rubric rubric = rubricRepository.findById(query.rubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric yêu cầu."));

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("BẢO MẬT: Bộ Rubric này thuộc về Trường học. System Admin không được quyền tìm kiếm các phiên bản của nó.");
        }

        // 3. Call the database for the paged search (SHARED REPO FUNCTION)
        PageResult<RubricVersion> pageResult = rubricVersionRepository.searchRubricVersions(
                query.rubricId(),
                query.keyword(),
                query.status() == null ? null : query.status().name(),
                query.page(),
                query.size()
        );

        // 4. Map back to DTOs to return to the caller
        return new PageResult<>(
                pageResult.content().stream()
                        .map(RubricVersionDtoMapper::toRubricVersionDto)
                        .toList(),
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages()
        );
    }
}