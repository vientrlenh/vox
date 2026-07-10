package com.sep.vox.application.port.input.usecase.framework;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.framework.FrameworkCriterionDtoMapper;
import com.sep.vox.application.port.input.query.ViewFrameworkCriteriaQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.dto.FrameworkCriterionDto;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

// Query gốc cho System Admin: lấy toàn bộ FrameworkCriterion của 1 FrameworkVersion bất kỳ trạng thái
// (System Admin được xem cả DRAFT vì họ là người biên soạn Framework).
@Service
public class ViewFrameworkCriteriaUseCase implements IUseCase<ViewFrameworkCriteriaQuery, List<FrameworkCriterionDto>> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public ViewFrameworkCriteriaUseCase(
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkCriterionBandRepository frameworkCriterionBandRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkCriterionBandRepository = frameworkCriterionBandRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FrameworkCriterionDto> execute(ViewFrameworkCriteriaQuery query) {
        // 1. Kiểm tra FrameworkVersion tồn tại
        frameworkVersionRepository.findById(query.frameworkVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Framework."));

        // 2. Lấy Criteria + Band tương ứng, map sang DTO
        var criteria = frameworkCriterionRepository.findByFrameworkVersionId(query.frameworkVersionId());
        var criterionIds = criteria.stream().map(c -> c.getId()).toList();
        var bandsByCriterionId = frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(criterionIds)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(b -> b.getFrameworkCriterionId()));

        return criteria.stream()
                .map(c -> FrameworkCriterionDtoMapper.toDto(c, bandsByCriterionId.getOrDefault(c.getId(), List.of()), jsonSerializationPort))
                .toList();
    }
}