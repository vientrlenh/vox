package com.sep.vox.application.port.input.usecase.framework;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateFrameworkCriterionBandCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

@Service
public class UpdateFrameworkCriterionBandUseCase
        implements IUseCase<UpdateFrameworkCriterionBandCommand, UUID> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private final UserContextPort userContextPort;

    public UpdateFrameworkCriterionBandUseCase(
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkCriterionBandRepository frameworkCriterionBandRepository,
            UserContextPort userContextPort) {
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkCriterionBandRepository = frameworkCriterionBandRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkCriterionBandCommand command) {
        FrameworkVersion version = getVersion(command);
        FrameworkCriterion criterion = getCriterion(command);
        FrameworkCriterionBand band = getBand(command);

        checkValidRequest(command, version, criterion, band);
        updateCriterionBand(command, band);

        frameworkCriterionBandRepository.save(band);
        return band.getId();
    }

    private FrameworkVersion getVersion(UpdateFrameworkCriterionBandCommand command) {
        return frameworkVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản khung năng lực"));
    }

    private FrameworkCriterion getCriterion(UpdateFrameworkCriterionBandCommand command) {
        return frameworkCriterionRepository.findById(command.criterionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy tiêu chí"));
    }

    private FrameworkCriterionBand getBand(UpdateFrameworkCriterionBandCommand command) {
        return frameworkCriterionBandRepository.findById(command.bandId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy mức đánh giá"));
    }

    private void checkValidRequest(UpdateFrameworkCriterionBandCommand command, FrameworkVersion version,
            FrameworkCriterion criterion, FrameworkCriterionBand band) {
        if (!version.getFrameworkId().equals(command.frameworkId()))
            throw new IllegalArgumentException("Phiên bản không thuộc khung năng lực này");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalStateException("Chỉ có thể cập nhật mức đánh giá khi phiên bản đang ở trạng thái DRAFT");

        if (!criterion.getFrameworkVersionId().equals(command.versionId()))
            throw new IllegalArgumentException("Tiêu chí không thuộc phiên bản này");

        if (!band.getFrameworkCriterionId().equals(command.criterionId()))
            throw new IllegalArgumentException("Mức đánh giá không thuộc tiêu chí này");
    }

    private void updateCriterionBand(UpdateFrameworkCriterionBandCommand command, FrameworkCriterionBand band) {
        if (hasText(command.descriptor()))
            band.setDescriptor(StringNormalization.trimAndCollapseSpaces(command.descriptor()));

        if (hasSignals(command.positiveSignals())) 
            band.setPositiveSignals(command.positiveSignals());

        if (hasSignals(command.negativeSignals())) 
            band.setNegativeSignals(command.negativeSignals());

        band.setUpdatedAt(OffsetDateTime.now());
        band.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
    }

    private boolean hasText(String request) {
        return request != null && !request.isEmpty();
    }

    private boolean hasSignals(FrameworkCriterionSignals signals) {
        return signals != null && !signals.values().isEmpty();
    }
}
