package com.sep.vox.application.port.input.usecase.framework;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class UpdateFrameworkResultBandUseCase
        implements IUseCase<UpdateFrameworkResultBandCommand, UUID> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final UserContextPort userContextPort;

    public UpdateFrameworkResultBandUseCase(
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            UserContextPort userContextPort) {
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkResultBandCommand command) {
        FrameworkVersion version = getVersion(command);
        FrameworkResultBand band = getBand(command);

        checkValidRequest(command, version, band);
        updateResultBand(command, band);

        try {
            frameworkResultBandRepository.save(band);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Mã hoặc nhãn kết quả đã tồn tại cho phiên bản này", e);
        }

        return band.getId();
    }

    private FrameworkVersion getVersion(UpdateFrameworkResultBandCommand command) {
        return frameworkVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản khung năng lực"));
    }

    private FrameworkResultBand getBand(UpdateFrameworkResultBandCommand command) {
        return frameworkResultBandRepository.findById(command.bandId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy mức kết quả"));
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
        if (frameworkResultBandRepository.existsByFrameworkVersionIdAndCodeAndIdNot(command.versionId(), safeCode, command.bandId())) {
            throw new IllegalStateException("Mã kết quả đã tồn tại");
        }
        if (frameworkResultBandRepository.existsByFrameworkVersionIdAndLabelAndIdNot(command.versionId(), safeLabel, command.bandId())) {
            throw new IllegalStateException("Nhãn kết quả đã tồn tại");
        }

        Set<Integer> otherOrders = frameworkResultBandRepository.findByFrameworkVersionId(command.versionId())
                .stream()
                .filter(frb -> !frb.getId().equals(command.bandId()))
                .map(frb -> frb.getOrder())
                .collect(Collectors.toSet());
        if (!otherOrders.add(command.order())) {
            throw new IllegalArgumentException("Thứ tự mức kết quả bị trùng lặp: " + command.order());
        }
        int expectedOrder = otherOrders.size();
        for (int i = 1; i <= expectedOrder; i++) {
            if (!otherOrders.contains(i)) {
                throw new IllegalArgumentException("Thứ tự mức kết quả phải tăng dần liên tục từ 1, không được bỏ số");
            }
        }
    }

    private void updateResultBand(UpdateFrameworkResultBandCommand command, FrameworkResultBand band) {
        band.setCode(StringNormalization.normalizeCode(command.code()));
        band.setLabel(StringNormalization.trimAndCollapseSpaces(command.label()));
        band.setDescription(StringNormalization.trimAndCollapseSpaces(command.description()));
        band.setOrder(command.order());
        band.setUpdatedAt(Instant.now());
        band.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
    }
}
