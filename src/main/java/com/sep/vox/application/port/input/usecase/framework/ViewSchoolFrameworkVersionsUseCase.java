package com.sep.vox.application.port.input.usecase.framework;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewFrameworkVersionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.FrameworkVersionDto;
import com.sep.vox.domain.mapper.FrameworkVersionDtoMapper;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

// Query gốc cho School Admin: chỉ được xem danh sách FrameworkVersion đã PUBLISHED
// (đối xứng với ViewSchoolFrameworkCriteriaUseCase).
@Service
public class ViewSchoolFrameworkVersionsUseCase implements IUseCase<ViewFrameworkVersionsQuery, PageResult<FrameworkVersionDto>> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;

    public ViewSchoolFrameworkVersionsUseCase(
            FrameworkRepository frameworkRepository,
            FrameworkVersionRepository frameworkVersionRepository) {
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
    }

    @Override
    public PageResult<FrameworkVersionDto> execute(ViewFrameworkVersionsQuery input) {
        frameworkRepository.findById(input.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));

        var result = frameworkVersionRepository.findByFrameworkIdAndStatus(
            input.frameworkId(), FrameworkVersionStatus.PUBLISHED, input.page(), input.size());
        return FrameworkVersionDtoMapper.toDtoPage(result);
    }
}
