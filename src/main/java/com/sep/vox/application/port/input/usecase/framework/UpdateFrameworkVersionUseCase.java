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
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class UpdateFrameworkVersionUseCase implements IUseCase<UpdateFrameworkVersionCommand, UUID> {

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
    public UUID execute(UpdateFrameworkVersionCommand input) {
        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        frameworkRepository.findById(input.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));

        var version = frameworkVersionRepository.findByIdForUpdate(input.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản framework"));

        if (!version.getFrameworkId().equals(input.frameworkId())) {
            throw new IllegalArgumentException("Phiên bản không thuộc framework này");
        }

        if (version.getStatus() != FrameworkVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể cập nhật phiên bản ở trạng thái DRAFT");
        }

        if (input.effectiveTo() != null && input.effectiveTo().isBefore(input.effectiveFrom())) {
            throw new IllegalArgumentException("Ngày hết hiệu lực phải sau ngày hiệu lực");
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
            replaceCriteria(input.versionId(), input.criteria(), now, currentUserId);
        }

        return input.versionId();
    }

    private void replaceResultBands(UUID versionId, List<UpdateFrameworkVersionCommand.ResultBandInput> bandInputs,
            OffsetDateTime now, UUID userId) {
        var codes = bandInputs.stream().map(b -> StringNormalization.normalizeCode(b.code())).toList();
        if (codes.size() != codes.stream().distinct().count())
            throw new IllegalArgumentException("Mã kết quả bị trùng lặp");
        frameworkCriterionBandRepository.deleteByFrameworkVersionId(versionId);
        frameworkResultBandRepository.deleteByFrameworkVersionId(versionId);
        var bands = bandInputs.stream()
            .map(b -> new FrameworkResultBand(
                versionId,
                StringNormalization.normalizeCode(b.code()),
                StringNormalization.trimAndCollapseSpaces(b.label()),
                StringNormalization.trimAndCollapseSpaces(b.description()),
                b.order(),
                now, now, userId, userId))
            .toList();
        frameworkResultBandRepository.saveAll(bands);
    }

    private void replaceCriteria(UUID versionId, List<UpdateFrameworkVersionCommand.CriterionInput> criterionInputs,
            OffsetDateTime now, UUID userId) {
        var codes = criterionInputs.stream().map(c -> StringNormalization.normalizeCode(c.code())).toList();
        if (codes.size() != codes.stream().distinct().count())
            throw new IllegalArgumentException("Mã tiêu chí bị trùng lặp");
        var existingCriteria = frameworkCriterionRepository.findByFrameworkVersionId(versionId);
        if (!existingCriteria.isEmpty()) {
            frameworkCriterionBandRepository.deleteByFrameworkCriterionIdIn(
                existingCriteria.stream().map(FrameworkCriterion::getId).toList());
        }
        frameworkCriterionRepository.deleteByFrameworkVersionId(versionId);

        var savedResultBands = frameworkResultBandRepository.findByFrameworkVersionId(versionId);
        Map<String, UUID> resultBandCodeToId = savedResultBands.stream()
            .collect(Collectors.toMap(FrameworkResultBand::getCode, FrameworkResultBand::getId));

        var criteriaToSave = criterionInputs.stream()
            .map(c -> new FrameworkCriterion(
                versionId,
                StringNormalization.normalizeCode(c.code()),
                StringNormalization.trimAndCollapseSpaces(c.name()),
                StringNormalization.trimAndCollapseSpaces(c.description()),
                c.order(),
                now, now, userId, userId))
            .toList();
        var savedCriteria = frameworkCriterionRepository.saveAll(criteriaToSave);
        Map<String, UUID> criterionCodeToId = savedCriteria.stream()
            .collect(Collectors.toMap(FrameworkCriterion::getCode, FrameworkCriterion::getId));

        List<FrameworkCriterionBand> allBands = new ArrayList<>();
        for (var criterionInput : criterionInputs) {
            var bands = criterionInput.bands();
            if (bands == null || bands.isEmpty()) continue;
            var savedId = criterionCodeToId.get(StringNormalization.normalizeCode(criterionInput.code()));
            if (savedId == null)
                throw new IllegalArgumentException("Không tìm thấy tiêu chí với mã: " + criterionInput.code());
            for (var bandInput : bands) {
                var resultBandId = resultBandCodeToId.get(StringNormalization.normalizeCode(bandInput.resultBandCode()));
                if (resultBandId == null)
                    throw new IllegalArgumentException("Không tìm thấy kết quả với mã: " + bandInput.resultBandCode());
                allBands.add(new FrameworkCriterionBand(
                    savedId,
                    resultBandId,
                    StringNormalization.trimAndCollapseSpaces(bandInput.descriptor()),
                    bandInput.positiveSignals(),
                    bandInput.negativeSignals(),
                    now, now, userId, userId));
            }
        }
        if (!allBands.isEmpty()) {
            frameworkCriterionBandRepository.saveAll(allBands);
        }
    }
}
