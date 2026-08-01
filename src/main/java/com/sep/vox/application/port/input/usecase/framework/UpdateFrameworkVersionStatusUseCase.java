package com.sep.vox.application.port.input.usecase.framework;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateFrameworkVersionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class UpdateFrameworkVersionStatusUseCase implements IUseCase<UpdateFrameworkVersionStatusCommand, UUID> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;

    public UpdateFrameworkVersionStatusUseCase(
            FrameworkRepository frameworkRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            FrameworkCriterionBandRepository frameworkCriterionBandRepository) {
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.frameworkCriterionBandRepository = frameworkCriterionBandRepository;
    }

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkVersionStatusCommand input) {
        getFramework(input);
        FrameworkVersion version = getVersion(input);

        checkValidRequest(input, version);
        int updated = frameworkVersionRepository.updateStatus(input.versionId(), input.status());
        if (updated == 0) {
            throw new NotFoundException("Không tìm thấy phiên bản framework để cập nhật trạng thái");
        }
        return input.versionId();
    }

    private void getFramework(UpdateFrameworkVersionStatusCommand input) {
        frameworkRepository.findFrameworkByIdForUpdate(input.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));
    }

    private FrameworkVersion getVersion(UpdateFrameworkVersionStatusCommand input) {
        return frameworkVersionRepository.findByIdForUpdate(input.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản framework"));
    }

    private void checkValidRequest(UpdateFrameworkVersionStatusCommand input, FrameworkVersion version) {
        if (!version.getFrameworkId().equals(input.frameworkId()))
            throw new IllegalArgumentException("Phiên bản không thuộc framework này");

        if (input.status() == FrameworkVersionStatus.PUBLISHED) {
            if (version.getStatus() != FrameworkVersionStatus.DRAFT)
                throw new IllegalStateException("Chỉ có thể xuất bản phiên bản ở trạng thái DRAFT");
            if (!frameworkCriterionRepository.existsByFrameworkVersionId(input.versionId()))
                throw new IllegalStateException("Phiên bản framework phải có ít nhất một tiêu chí trước khi xuất bản");
            if (!frameworkResultBandRepository.existsByFrameworkVersionId(input.versionId()))
                throw new IllegalStateException("Phiên bản framework phải có ít nhất một dải kết quả trước khi xuất bản");
            validateEveryCriterionHasBandsWithSignals(input.versionId());
            validateNoConflictingPublished(input.frameworkId(), input.versionId(), version.getEffectiveFrom(), version.getEffectiveTo());
        } else if (input.status() == FrameworkVersionStatus.ARCHIVED) {
            if (version.getStatus() != FrameworkVersionStatus.PUBLISHED)
                throw new IllegalStateException("Chỉ có thể lưu trữ phiên bản ở trạng thái PUBLISHED");
        } else {
            throw new IllegalArgumentException("Trạng thái không hợp lệ để cập nhật");
        }
    }

    private void validateEveryCriterionHasBandsWithSignals(UUID versionId) {
        List<FrameworkCriterion> criteria = frameworkCriterionRepository.findByFrameworkVersionId(versionId);
        List<UUID> criterionIds = criteria.stream().map(fc -> fc.getId()).collect(Collectors.toList());
        List<FrameworkCriterionBand> bands = frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(criterionIds);

        var bandsByCriterion = bands.stream().collect(Collectors.groupingBy(fcb -> fcb.getFrameworkCriterionId()));

        for (FrameworkCriterion criterion : criteria) {
            List<FrameworkCriterionBand> criterionBands = bandsByCriterion.getOrDefault(criterion.getId(), List.of());
            if (criterionBands.isEmpty()) {
                throw new IllegalStateException(
                        "Tiêu chí \"" + criterion.getName() + "\" phải có ít nhất một thang kết quả trước khi xuất bản");
            }
            for (FrameworkCriterionBand band : criterionBands) {
                if (band.getPositiveSignals() == null || band.getPositiveSignals().values().isEmpty()
                        || band.getNegativeSignals() == null || band.getNegativeSignals().values().isEmpty()) {
                    throw new IllegalStateException(
                            "Tiêu chí \"" + criterion.getName() + "\" phải có ít nhất một dấu hiệu tích cực và tiêu cực ở mỗi thang kết quả trước khi xuất bản");
                }
            }
        }
    }

    private void validateNoConflictingPublished(UUID frameworkId, UUID versionId, Instant effectiveFrom, Instant effectiveTo) {
        var published = frameworkVersionRepository.findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED);
        boolean hasConflict = published.stream()
            .filter(v -> !v.getId().equals(versionId))
            .anyMatch(v -> rangesOverlap(effectiveFrom, effectiveTo, v.getEffectiveFrom(), v.getEffectiveTo()));
        if (hasConflict) {
            throw new IllegalStateException("Đã tồn tại phiên bản đã xuất bản có hiệu lực trong khoảng thời gian này");
        }
    }

    private boolean rangesOverlap(Instant from1, Instant to1, Instant from2, Instant to2) {
        Instant start1 = from1 != null ? from1 : Instant.MIN;
        Instant start2 = from2 != null ? from2 : Instant.MIN;
        Instant end1 = to1 != null ? to1 : Instant.MAX;
        Instant end2 = to2 != null ? to2 : Instant.MAX;
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}
