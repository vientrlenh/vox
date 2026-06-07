package com.sep.vox.application.port.input.usecase.framework;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkResultBandStatus;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class UpdateFrameworkVersionUseCase implements IUseCase<UpdateFrameworkVersionCommand, Void> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final UserContextPort userContextPort;

    public UpdateFrameworkVersionUseCase(
            FrameworkRepository frameworkRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkCriterionBandRepository frameworkCriterionBandRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            UserContextPort userContextPort) {
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkCriterionBandRepository = frameworkCriterionBandRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(UpdateFrameworkVersionCommand input) {
        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        frameworkRepository.findById(input.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));

        var version = frameworkVersionRepository.findById(input.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản framework"));

        if (!version.getFrameworkId().equals(input.frameworkId())) {
            throw new IllegalArgumentException("Phiên bản không thuộc framework này");
        }

        if (version.getStatus() != FrameworkVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể cập nhật phiên bản ở trạng thái DRAFT");
        }

        var updated = new FrameworkVersion(
            version.getId(),
            version.getFrameworkId(),
            StringNormalization.normalizeCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
            version.getVersion(),
            input.effectiveFrom(),
            input.effectiveTo(),
            version.getStatus(),
            version.getCreatedAt(),
            now,
            version.getCreatedBy(),
            currentUserId
        );
        frameworkVersionRepository.save(updated);

        if (input.resultBands() != null) {
            replaceResultBands(input.versionId(), input.resultBands(), now, currentUserId);
        }

        if (input.criteria() != null) {
            replaceCriteria(input.versionId(), input.criteria(), input.resultBands(), now, currentUserId);
        }

        return null;
    }

    private void replaceResultBands(UUID versionId, List<UpdateFrameworkVersionCommand.ResultBandInput> bandInputs,
            OffsetDateTime now, UUID userId) {
        frameworkResultBandRepository.deleteByFrameworkVersionId(versionId);
        var bands = bandInputs.stream()
            .map(b -> new FrameworkResultBand(
                versionId,
                StringNormalization.normalizeCode(b.code()),
                StringNormalization.trimAndCollapseSpaces(b.label()),
                StringNormalization.trimAndCollapseSpaces(b.description()),
                b.scoreMin(),
                b.scoreMax(),
                b.order(),
                FrameworkResultBandStatus.DRAFT,
                now, now, userId, userId))
            .toList();
        frameworkResultBandRepository.saveAll(bands);
    }

    private void replaceCriteria(UUID versionId, List<UpdateFrameworkVersionCommand.CriterionInput> criterionInputs,
            List<UpdateFrameworkVersionCommand.ResultBandInput> resultBandInputs, OffsetDateTime now, UUID userId) {
        var existingCriteria = frameworkCriterionRepository.findByFrameworkVersionId(versionId);
        for (var c : existingCriteria) {
            frameworkCriterionBandRepository.deleteByFrameworkCriterionId(c.getId());
        }
        frameworkCriterionRepository.deleteByFrameworkVersionId(versionId);

        var savedResultBands = frameworkResultBandRepository.findByFrameworkVersionId(versionId);
        Map<String, UUID> resultBandCodeToId = savedResultBands.stream()
            .collect(Collectors.toMap(rb -> rb.getCode(), rb -> rb.getId()));

        for (var criterionInput : criterionInputs) {
            var criterion = new FrameworkCriterion(
                versionId,
                StringNormalization.normalizeCode(criterionInput.code()),
                StringNormalization.trimAndCollapseSpaces(criterionInput.name()),
                StringNormalization.trimAndCollapseSpaces(criterionInput.description()),
                now, now, userId, userId);
            var savedCriterion = frameworkCriterionRepository.save(criterion);

            if (criterionInput.bands() != null && !criterionInput.bands().isEmpty()) {
                List<FrameworkCriterionBand> criterionBands = new ArrayList<>();
                for (var bandInput : criterionInput.bands()) {
                    var resultBandId = resultBandCodeToId.get(bandInput.resultBandCode());
                    if (resultBandId == null) continue;
                    criterionBands.add(new FrameworkCriterionBand(
                        savedCriterion.getId(),
                        resultBandId,
                        StringNormalization.trimAndCollapseSpaces(bandInput.descriptor()),
                        StringNormalization.trimAndCollapseSpaces(bandInput.positiveSignals()),
                        StringNormalization.trimAndCollapseSpaces(bandInput.negativeSignals()),
                        now, now, userId, userId));
                }
                if (!criterionBands.isEmpty()) {
                    frameworkCriterionBandRepository.saveAll(criterionBands);
                }
            }
        }
    }
}
