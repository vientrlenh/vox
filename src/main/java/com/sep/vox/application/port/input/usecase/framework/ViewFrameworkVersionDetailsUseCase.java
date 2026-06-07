package com.sep.vox.application.port.input.usecase.framework;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewFrameworkVersionDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.FrameworkVersionDto;
import com.sep.vox.domain.mapper.FrameworkVersionDtoMapper;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class ViewFrameworkVersionDetailsUseCase implements IUseCase<ViewFrameworkVersionDetailsQuery, FrameworkVersionDto> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;

    public ViewFrameworkVersionDetailsUseCase(
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkCriterionBandRepository frameworkCriterionBandRepository,
            FrameworkResultBandRepository frameworkResultBandRepository) {
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkCriterionBandRepository = frameworkCriterionBandRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public FrameworkVersionDto execute(ViewFrameworkVersionDetailsQuery input) {
        var version = frameworkVersionRepository.findById(input.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản framework"));

        var criteria = frameworkCriterionRepository.findByFrameworkVersionId(version.getId());

        var allBands = criteria.stream()
            .flatMap(c -> frameworkCriterionBandRepository.findByFrameworkCriterionId(c.getId()).stream())
            .toList();

        var resultBands = frameworkResultBandRepository.findByFrameworkVersionId(version.getId());

        return FrameworkVersionDtoMapper.toDto(version, criteria, allBands, resultBands);
    }
}
