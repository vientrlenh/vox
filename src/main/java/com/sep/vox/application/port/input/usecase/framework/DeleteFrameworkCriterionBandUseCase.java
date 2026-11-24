package com.sep.vox.application.port.input.usecase.framework;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.framework.DeleteFrameworkCriterionBandCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
@RequiredArgsConstructor
public class DeleteFrameworkCriterionBandUseCase
        implements IUseCase<DeleteFrameworkCriterionBandCommand, Void> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;

    @Override
    @Transactional
    public Void execute(DeleteFrameworkCriterionBandCommand command) {
        FrameworkVersion version = getVersion(command);
        FrameworkCriterion criterion = getCriterion(command);
        FrameworkCriterionBand band = getBand(command);

        checkValidRequest(command, version, criterion, band);
        frameworkCriterionBandRepository.deleteById(command.bandId());
        return null;
    }

    private FrameworkVersion getVersion(DeleteFrameworkCriterionBandCommand command) {
        return frameworkVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản khung năng lực"));
    }

    private FrameworkCriterion getCriterion(DeleteFrameworkCriterionBandCommand command) {
        return frameworkCriterionRepository.findById(command.criterionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tiêu chí"));
    }

    private FrameworkCriterionBand getBand(DeleteFrameworkCriterionBandCommand command) {
        return frameworkCriterionBandRepository.findById(command.bandId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mức đánh giá"));
    }

    private void checkValidRequest(DeleteFrameworkCriterionBandCommand command, FrameworkVersion version,
            FrameworkCriterion criterion, FrameworkCriterionBand band) {
        if (!version.getFrameworkId().equals(command.frameworkId()))
            throw new IllegalArgumentException("Phiên bản không thuộc framework này");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalArgumentException("Chỉ có thể chỉnh sửa phiên bản ở trạng thái DRAFT");

        if (!criterion.getFrameworkVersionId().equals(command.versionId()))
            throw new IllegalArgumentException("Tiêu chí không thuộc phiên bản này");

        if (!band.getFrameworkCriterionId().equals(command.criterionId()))
            throw new IllegalArgumentException("Mức đánh giá không thuộc tiêu chí này");
    }
}
