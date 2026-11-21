package com.sep.vox.application.port.input.usecase.framework;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class UpdateFrameworkVersionUseCase implements IUseCase<UpdateFrameworkVersionCommand, UUID> {

    private final FrameworkVersionRepository frameworkVersionRepository;

    public UpdateFrameworkVersionUseCase(
            FrameworkVersionRepository frameworkVersionRepository) {
        this.frameworkVersionRepository = frameworkVersionRepository;
    }

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkVersionCommand input) {
        FrameworkVersion version = getVersion(input);

        checkValidRequest(input, version);
        frameworkVersionRepository.save(updateVersion(input, version));

        return input.versionId();
    }

    private FrameworkVersion getVersion(UpdateFrameworkVersionCommand input) {
        return frameworkVersionRepository.findByIdForUpdate(input.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản framework"));
    }

    private FrameworkVersion updateVersion(UpdateFrameworkVersionCommand input, FrameworkVersion version) {
        return FrameworkVersion.builder()
            .id(version.getId())
            .frameworkId(version.getFrameworkId())
            .code(StringNormalization.normalizeCode(input.code()))
            .name(StringNormalization.trimAndCollapseSpaces(input.name()))
            .description(StringNormalization.trimAndCollapseSpaces(input.description()))
            .version(version.getVersion())
            .effectiveFrom(input.effectiveFrom())
            .effectiveTo(input.effectiveTo())
            .status(version.getStatus())
            .createdAt(version.getCreatedAt())
            .createdBy(version.getCreatedBy())
            .build();
    }

    private void checkValidRequest(UpdateFrameworkVersionCommand input, FrameworkVersion version) {
        if (!version.getFrameworkId().equals(input.frameworkId()))
            throw new IllegalArgumentException("Phiên bản không thuộc framework này");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalStateException("Chỉ có thể cập nhật phiên bản framework ở trạng thái DRAFT");

        if (input.effectiveTo() != null && !input.effectiveTo().isAfter(input.effectiveFrom()))
            throw new IllegalArgumentException("Ngày hết hiệu lực phải sau ngày hiệu lực");
    }
}
