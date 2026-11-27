package com.sep.vox.application.port.input.usecase.framework;

import com.sep.vox.domain.model.framework.FrameworkCriterion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.framework.ViewFrameworkCriterionDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.FrameworkCriterionDto;
import com.sep.vox.domain.mapper.FrameworkCriterionDtoMapper;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;

// Root query for System Admin: details of one FrameworkCriterion by ID, regardless of the Version status.
@Service
@RequiredArgsConstructor
public class ViewFrameworkCriterionDetailsUseCase implements IUseCase<ViewFrameworkCriterionDetailsQuery, FrameworkCriterionDto> {

    private final FrameworkCriterionRepository frameworkCriterionRepository;

    @Override
    @Transactional(readOnly = true)
    public FrameworkCriterionDto execute(ViewFrameworkCriterionDetailsQuery query) {
        FrameworkCriterion criterion = frameworkCriterionRepository.findById(query.criterionId())
                .orElseThrow(() -> new ResourceNotFoundException("Criterion not found."));

        return FrameworkCriterionDtoMapper.toDto(criterion);
    }
}