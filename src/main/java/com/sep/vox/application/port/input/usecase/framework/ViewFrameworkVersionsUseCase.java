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

@Service
public class ViewFrameworkVersionsUseCase implements IUseCase<ViewFrameworkVersionsQuery, PageResult<FrameworkVersionDto>> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;

    public ViewFrameworkVersionsUseCase(
            FrameworkRepository frameworkRepository,
            FrameworkVersionRepository frameworkVersionRepository) {
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
    }

    @Override
    public PageResult<FrameworkVersionDto> execute(ViewFrameworkVersionsQuery input) {
        frameworkRepository.findById(input.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));

        var result = (input.status() != null && !input.status().isBlank())
            ? frameworkVersionRepository.findByFrameworkIdAndStatus(
                input.frameworkId(), parseStatus(input.status()), input.page(), input.size())
            : frameworkVersionRepository.findByFrameworkId(input.frameworkId(), input.page(), input.size());
        return FrameworkVersionDtoMapper.toDtoPage(result);
    }

    private FrameworkVersionStatus parseStatus(String status) {
        try {
            return FrameworkVersionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái (status) không hợp lệ. Chỉ chấp nhận DRAFT, PUBLISHED, ARCHIVED.");
        }
    }
}
