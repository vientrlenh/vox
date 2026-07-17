package com.sep.vox.application.port.input.usecase.framework;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewFrameworkCriteriaQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.FrameworkCriterionDto;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

// Query gốc cho School Admin: chỉ được xem Criteria của FrameworkVersion đã PUBLISHED
// (Framework là tài nguyên toàn hệ thống, School không sở hữu nên không cần chốt chặn theo schoolId,
// chỉ cần chặn không cho lộ Version còn DRAFT/ARCHIVED chưa sẵn sàng dùng).
@Service
public class ViewSchoolFrameworkCriteriaUseCase implements IUseCase<ViewFrameworkCriteriaQuery, List<FrameworkCriterionDto>> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;

    public ViewSchoolFrameworkCriteriaUseCase(
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkCriterionBandRepository frameworkCriterionBandRepository) {
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkCriterionBandRepository = frameworkCriterionBandRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FrameworkCriterionDto> execute(ViewFrameworkCriteriaQuery query) {
        // 1. Kiểm tra FrameworkVersion tồn tại và đã PUBLISHED
        var version = frameworkVersionRepository.findById(query.frameworkVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Framework."));
        if (version.getStatus() != FrameworkVersionStatus.PUBLISHED) {
            throw new NotFoundException("Không tìm thấy phiên bản Framework.");
        }

        // 2. Lấy Criteria + Band tương ứng, map sang DTO
        var criteria = frameworkCriterionRepository.findByFrameworkVersionId(query.frameworkVersionId());
        var criterionIds = criteria.stream().map(c -> c.getId()).toList();
        var bandsByCriterionId = frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(criterionIds)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(b -> b.getFrameworkCriterionId()));

        return criteria.stream()
                .map(c -> FrameworkCriterionDto.of(c, bandsByCriterionId.getOrDefault(c.getId(), List.of())))
                .toList();
    }
}