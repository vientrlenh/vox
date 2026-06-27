package com.sep.vox.application.port.input.usecase.framework;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateFrameworkVersionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class UpdateFrameworkVersionStatusUseCase implements IUseCase<UpdateFrameworkVersionStatusCommand, UUID> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;

    public UpdateFrameworkVersionStatusUseCase(
            FrameworkRepository frameworkRepository,
            FrameworkVersionRepository frameworkVersionRepository) {
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
    }

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkVersionStatusCommand input) {
        frameworkRepository.findById(input.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));

        var version = frameworkVersionRepository.findByIdForUpdate(input.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản framework"));

        if (!version.getFrameworkId().equals(input.frameworkId())) {
            throw new IllegalArgumentException("Phiên bản không thuộc framework này");
        }

        if (input.status() == FrameworkVersionStatus.PUBLISHED) {
            if (version.getStatus() != FrameworkVersionStatus.DRAFT) {
                throw new IllegalStateException("Chỉ có thể xuất bản phiên bản ở trạng thái DRAFT");
            }
            validateNoConflictingPublished(input.frameworkId(), input.versionId(), version.getEffectiveFrom(), version.getEffectiveTo());
            frameworkVersionRepository.updateStatus(input.versionId(), FrameworkVersionStatus.PUBLISHED);
        } else if (input.status() == FrameworkVersionStatus.ARCHIVED) {
            frameworkVersionRepository.updateStatus(input.versionId(), FrameworkVersionStatus.ARCHIVED);
        } else {
            throw new IllegalArgumentException("Trạng thái không hợp lệ để cập nhật");
        }

        return input.versionId();
    }

    private void validateNoConflictingPublished(UUID frameworkId, UUID versionId, OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo) {
        var published = frameworkVersionRepository.findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED);
        boolean hasConflict = published.stream()
            .filter(v -> !v.getId().equals(versionId))
            .anyMatch(v -> rangesOverlap(effectiveFrom, effectiveTo, v.getEffectiveFrom(), v.getEffectiveTo()));
        if (hasConflict) {
            throw new IllegalStateException("Đã tồn tại phiên bản đã xuất bản có hiệu lực trong khoảng thời gian này");
        }
    }

    private boolean rangesOverlap(OffsetDateTime from1, OffsetDateTime to1, OffsetDateTime from2, OffsetDateTime to2) {
        OffsetDateTime end1 = to1 != null ? to1 : OffsetDateTime.MAX;
        OffsetDateTime end2 = to2 != null ? to2 : OffsetDateTime.MAX;
        return from1.isBefore(end2) && from2.isBefore(end1);
    }
}
