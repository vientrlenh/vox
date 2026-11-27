package com.sep.vox.application.port.input.usecase.framework;

import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.framework.ViewFrameworkCriteriaQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.FrameworkCriterionDto;
import com.sep.vox.domain.mapper.FrameworkCriterionDtoMapper;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

// Root query for School Admin: may only see the Criteria of a PUBLISHED FrameworkVersion
// (a Framework is a system-wide resource that a School does not own, so no schoolId guard is needed;
// it only has to keep DRAFT/ARCHIVED Versions, which are not ready for use, from leaking).
@Service
@RequiredArgsConstructor
public class ViewSchoolFrameworkCriteriaUseCase implements IUseCase<ViewFrameworkCriteriaQuery, List<FrameworkCriterionDto>> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FrameworkCriterionDto> execute(ViewFrameworkCriteriaQuery query) {
        FrameworkVersion version = frameworkVersionRepository.findById(query.frameworkVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Framework version not found."));
        if (version.getStatus() != FrameworkVersionStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Framework version not found.");
        }

        List<FrameworkCriterion> criteria = frameworkCriterionRepository.findByFrameworkVersionId(query.frameworkVersionId());

        return criteria.stream()
                .map(c -> FrameworkCriterionDtoMapper.toDto(c))
                .toList();
    }
}