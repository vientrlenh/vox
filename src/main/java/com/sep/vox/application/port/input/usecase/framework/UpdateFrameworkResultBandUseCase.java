package com.sep.vox.application.port.input.usecase.framework;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateFrameworkResultBandCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class UpdateFrameworkResultBandUseCase
        implements IUseCase<UpdateFrameworkResultBandCommand, UUID> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final UserContextPort userContextPort;

    public UpdateFrameworkResultBandUseCase(
            FrameworkRepository frameworkRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            UserContextPort userContextPort) {
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkResultBandCommand command) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        OffsetDateTime now = OffsetDateTime.now();

        frameworkRepository.findById(command.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy khung năng lực"));

        FrameworkVersion version = frameworkVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản khung năng lực"));

        FrameworkResultBand band = frameworkResultBandRepository.findById(command.bandId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy mức kết quả"));

        checkValidRequest(command, version, band);

        String safeCode = StringNormalization.normalizeCode(command.code());
        String safeLabel = StringNormalization.trimAndCollapseSpaces(command.label());

        band.setCode(safeCode);
        band.setLabel(safeLabel);
        band.setDescription(StringNormalization.trimAndCollapseSpaces(command.description()));
        band.setOrder(command.order());
        band.setUpdatedAt(now);
        band.setUpdatedBy(userId);

        try {
            frameworkResultBandRepository.save(band);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Mã hoặc nhãn kết quả đã tồn tại cho phiên bản này", e);
        }

        return band.getId();
    }

    private void checkValidRequest(UpdateFrameworkResultBandCommand command, FrameworkVersion version, FrameworkResultBand band) {
        if (!version.getFrameworkId().equals(command.frameworkId()))
            throw new IllegalArgumentException("Phiên bản không thuộc khung năng lực này");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalStateException("Chỉ có thể cập nhật mức kết quả khi phiên bản đang ở trạng thái DRAFT");

        if (!band.getFrameworkVersionId().equals(command.versionId()))
            throw new IllegalArgumentException("Mức kết quả không thuộc phiên bản này");

        String safeCode = StringNormalization.normalizeCode(command.code());
        String safeLabel = StringNormalization.trimAndCollapseSpaces(command.label());

        List<FrameworkResultBand> existingBands = frameworkResultBandRepository
            .findByFrameworkVersionId(command.versionId());
        for (FrameworkResultBand other : existingBands) {
            if (other.getId().equals(band.getId())) {
                continue;
            }
            if (StringNormalization.normalizeCode(other.getCode()).equals(safeCode)) {
                throw new IllegalArgumentException("Mã kết quả đã tồn tại: " + safeCode);
            }
            if (StringNormalization.trimAndCollapseSpaces(other.getLabel()).equals(safeLabel)) {
                throw new IllegalArgumentException("Nhãn kết quả đã tồn tại: " + safeLabel);
            }
        }
    }
}
