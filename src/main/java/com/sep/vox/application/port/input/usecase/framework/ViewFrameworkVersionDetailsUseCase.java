package com.sep.vox.application.port.input.usecase.framework;

import com.sep.vox.domain.model.framework.FrameworkVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.ViewFrameworkVersionDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.FrameworkVersionDto;
import com.sep.vox.domain.mapper.FrameworkVersionDtoMapper;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
@RequiredArgsConstructor
public class ViewFrameworkVersionDetailsUseCase implements IUseCase<ViewFrameworkVersionDetailsQuery, FrameworkVersionDto> {

    private final FrameworkVersionRepository frameworkVersionRepository;

    @Override
    public FrameworkVersionDto execute(ViewFrameworkVersionDetailsQuery input) {
        FrameworkVersion version = frameworkVersionRepository.findById(input.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản framework"));
        return FrameworkVersionDtoMapper.toDto(version);
    }
}
