package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.mapper.rubric.RubricCriterionDtoMapper;
import com.sep.vox.application.port.input.query.rubricsystem.SearchSystemRubricCriteriaQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricCriterionDto;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchSystemRubricCriteriaUseCase implements IUseCase<SearchSystemRubricCriteriaQuery, PageResult<RubricCriterionDto>> {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final RubricCriterionDtoMapper rubricCriterionDtoMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<RubricCriterionDto> execute(SearchSystemRubricCriteriaQuery query) {
        // Xác thực user
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // Lấy Version kiểm tra tính tồn tại
        RubricVersion version = rubricVersionRepository.findById(query.versionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản Rubric."));

        // Kiểm tra tính sở hữu bảo mật
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("BẢO MẬT: Phiên bản này thuộc về Trường học. Bạn không có quyền xem.");
        }

        // Gọi DB tìm kiếm
        PageResult<RubricCriterion> pageResult = rubricCriterionRepository.searchRubricCriteria(
                query.versionId(), query.keyword(), query.isRequired(), query.page(), query.size()
        );

        return new PageResult<>(
                pageResult.content().stream()
                        .map(rubricCriterionDtoMapper::toDto)
                        .toList(),
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages()
        );
    }
}