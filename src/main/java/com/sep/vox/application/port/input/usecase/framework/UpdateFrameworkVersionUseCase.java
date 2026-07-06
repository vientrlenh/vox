package com.sep.vox.application.port.input.usecase.framework;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class UpdateFrameworkVersionUseCase implements IUseCase<UpdateFrameworkVersionCommand, UUID> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final UserContextPort userContextPort;

    public UpdateFrameworkVersionUseCase(
            FrameworkVersionRepository frameworkVersionRepository,
            UserContextPort userContextPort) {
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkVersionCommand input) {
        FrameworkVersion version = getVersion(input);

        checkValidRequest(input, version);
        frameworkVersionRepository.saveFrameworkVersion(updateVersion(input, version));

        return input.versionId();
    }

    private FrameworkVersion getVersion(UpdateFrameworkVersionCommand input) {
        return frameworkVersionRepository.findFrameworkVersionByIdForUpdate(input.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản framework"));
    }

    private FrameworkVersion updateVersion(UpdateFrameworkVersionCommand input, FrameworkVersion version) {
        return new FrameworkVersion(
            version.getId(), version.getFrameworkId(),
            StringNormalization.normalizeCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
            version.getVersion(), input.effectiveFrom(), input.effectiveTo(),
            version.getStatus(), 
            version.getCreatedAt(), OffsetDateTime.now(),
            version.getCreatedBy(), userContextPort.getCurrentAuthenticatedUserId());
    }

    private void checkValidRequest(UpdateFrameworkVersionCommand input, FrameworkVersion version) {
        if (!version.getFrameworkId().equals(input.frameworkId()))
            throw new IllegalArgumentException("Phiên bản không thuộc framework này");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalStateException("Chỉ có thể cập nhật phiên bản framework ở trạng thái DRAFT");

        if (input.effectiveTo() != null && input.effectiveTo().isBefore(input.effectiveFrom()))
            throw new IllegalArgumentException("Ngày hết hiệu lực phải sau ngày hiệu lực");
    }
}
