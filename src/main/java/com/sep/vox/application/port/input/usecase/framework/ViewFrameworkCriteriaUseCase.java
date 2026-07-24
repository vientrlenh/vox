package com.sep.vox.application.port.input.usecase.framework;

import java.util.List;

import org.springframework.stereotype.Service;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewFrameworkCriteriaQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.FrameworkCriterionDto;
import com.sep.vox.domain.mapper.FrameworkCriterionDtoMapper;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

// Query gốc cho System Admin: lấy toàn bộ FrameworkCriterion của 1 FrameworkVersion bất kỳ trạng thái
// (System Admin được xem cả DRAFT vì họ là người biên soạn Framework).
@Service
public class ViewFrameworkCriteriaUseCase implements IUseCase<ViewFrameworkCriteriaQuery, List<FrameworkCriterionDto>> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;

    public ViewFrameworkCriteriaUseCase(
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkCriterionRepository frameworkCriterionRepository) {
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
    }

    @Override
    public List<FrameworkCriterionDto> execute(ViewFrameworkCriteriaQuery query) {
        // 1. Kiểm tra FrameworkVersion tồn tại
        frameworkVersionRepository.findById(query.frameworkVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Framework."));

        // 2. Lấy Criteria + Band tương ứng, map sang DTO
        var criteria = frameworkCriterionRepository.findByFrameworkVersionId(query.frameworkVersionId());

        return criteria.stream()
                .map(c -> FrameworkCriterionDtoMapper.toDto(c))
                .toList();
    }
}