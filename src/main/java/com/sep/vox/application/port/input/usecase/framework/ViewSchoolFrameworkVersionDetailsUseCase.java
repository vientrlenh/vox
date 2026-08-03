package com.sep.vox.application.port.input.usecase.framework;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewFrameworkVersionDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.FrameworkVersionDto;
import com.sep.vox.domain.mapper.FrameworkVersionDtoMapper;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

// Query gốc cho School Admin: chỉ được xem chi tiết FrameworkVersion đã PUBLISHED
// (đối xứng với ViewSchoolFrameworkCriteriaUseCase).
@Service
public class ViewSchoolFrameworkVersionDetailsUseCase implements IUseCase<ViewFrameworkVersionDetailsQuery, FrameworkVersionDto> {

    private final FrameworkVersionRepository frameworkVersionRepository;

    public ViewSchoolFrameworkVersionDetailsUseCase(FrameworkVersionRepository frameworkVersionRepository) {
        this.frameworkVersionRepository = frameworkVersionRepository;
    }

    @Override
    public FrameworkVersionDto execute(ViewFrameworkVersionDetailsQuery input) {
        var version = frameworkVersionRepository.findById(input.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản framework"));
        if (version.getStatus() != FrameworkVersionStatus.PUBLISHED) {
            throw new NotFoundException("Không tìm thấy phiên bản framework");
        }
        return FrameworkVersionDtoMapper.toDto(version);
    }
}
