package com.sep.vox.application.port.input.usecase.framework;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateFrameworkVersionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
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

    public UpdateFrameworkVersionStatusUseCase(
            FrameworkRepository frameworkRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkResultBandRepository frameworkResultBandRepository) {
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
    }

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkVersionStatusCommand input) {
        getFramework(input);
        FrameworkVersion version = getVersion(input);

        checkValidRequest(input, version);
        int updated = frameworkVersionRepository.updateFrameworkVersionStatus(input.versionId(), input.status());
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
        return frameworkVersionRepository.findFrameworkVersionByIdForUpdate(input.versionId())
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
            validateNoConflictingPublished(input.frameworkId(), input.versionId(), version.getEffectiveFrom(), version.getEffectiveTo());
        } else if (input.status() == FrameworkVersionStatus.ARCHIVED) {
            if (version.getStatus() != FrameworkVersionStatus.PUBLISHED)
                throw new IllegalStateException("Chỉ có thể lưu trữ phiên bản ở trạng thái PUBLISHED");
        } else {
            throw new IllegalArgumentException("Trạng thái không hợp lệ để cập nhật");
        }
    }

    private void validateNoConflictingPublished(UUID frameworkId, UUID versionId, OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo) {
        var published = frameworkVersionRepository.findByFrameworkVersionIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED);
        boolean hasConflict = published.stream()
            .filter(v -> !v.getId().equals(versionId))
            .anyMatch(v -> rangesOverlap(effectiveFrom, effectiveTo, v.getEffectiveFrom(), v.getEffectiveTo()));
        if (hasConflict) {
            throw new IllegalStateException("Đã tồn tại phiên bản đã xuất bản có hiệu lực trong khoảng thời gian này");
        }
    }

    private boolean rangesOverlap(OffsetDateTime from1, OffsetDateTime to1, OffsetDateTime from2, OffsetDateTime to2) {
        OffsetDateTime start1 = from1 != null ? from1 : OffsetDateTime.MIN;
        OffsetDateTime start2 = from2 != null ? from2 : OffsetDateTime.MIN;
        OffsetDateTime end1 = to1 != null ? to1 : OffsetDateTime.MAX;
        OffsetDateTime end2 = to2 != null ? to2 : OffsetDateTime.MAX;
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}
