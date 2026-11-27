package com.sep.vox.application.port.input.usecase.framework;

import com.sep.vox.domain.model.framework.FrameworkCriterion;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.framework.ViewFrameworkCriteriaQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.FrameworkCriterionDto;
import com.sep.vox.domain.mapper.FrameworkCriterionDtoMapper;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

// Root query for System Admin: all FrameworkCriterion of any FrameworkVersion, whatever its status
// (System Admin may also see DRAFT because they author the Framework).
@Service
@RequiredArgsConstructor
public class ViewFrameworkCriteriaUseCase implements IUseCase<ViewFrameworkCriteriaQuery, List<FrameworkCriterionDto>> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;

    @Override
    public List<FrameworkCriterionDto> execute(ViewFrameworkCriteriaQuery query) {
        frameworkVersionRepository.findById(query.frameworkVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Framework version not found."));

        List<FrameworkCriterion> criteria = frameworkCriterionRepository.findByFrameworkVersionId(query.frameworkVersionId());

        return criteria.stream()
                .map(c -> FrameworkCriterionDtoMapper.toDto(c))
                .toList();
    }
}