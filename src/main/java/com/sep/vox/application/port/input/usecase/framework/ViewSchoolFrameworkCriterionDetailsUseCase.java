package com.sep.vox.application.port.input.usecase.framework;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewFrameworkCriterionDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.dto.FrameworkCriterionDto;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

// Query gốc cho School Admin: xem chi tiết 1 FrameworkCriterion, chỉ khi FrameworkVersion chứa nó đã PUBLISHED.
@Service
public class ViewSchoolFrameworkCriterionDetailsUseCase implements IUseCase<ViewFrameworkCriterionDetailsQuery, FrameworkCriterionDto> {

    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;

    public ViewSchoolFrameworkCriterionDetailsUseCase(
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkCriterionBandRepository frameworkCriterionBandRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkCriterionBandRepository = frameworkCriterionBandRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public FrameworkCriterionDto execute(ViewFrameworkCriterionDetailsQuery query) {
        // 1. Lấy Criterion ra
        var criterion = frameworkCriterionRepository.findById(query.criterionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Tiêu chí (Criterion) này."));

        // 2. Chặn không cho lộ Criterion thuộc Version chưa PUBLISHED (còn DRAFT/ARCHIVED)
        var version = frameworkVersionRepository.findById(criterion.getFrameworkVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Tiêu chí (Criterion) này."));
        if (version.getStatus() != FrameworkVersionStatus.PUBLISHED) {
            throw new NotFoundException("Không tìm thấy Tiêu chí (Criterion) này.");
        }

        var bands = frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(java.util.List.of(criterion.getId()));

        return FrameworkCriterionDto.of(criterion, bands);
    }
}