package com.sep.vox.application.port.input.usecase.framework;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateFrameworkResultBandsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class CreateFrameworkResultBandsUseCase
        implements IUseCase<CreateFrameworkResultBandsCommand, List<UUID>> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final UserContextPort userContextPort;

    public CreateFrameworkResultBandsUseCase(
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
    public List<UUID> execute(CreateFrameworkResultBandsCommand command) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        OffsetDateTime now = OffsetDateTime.now();

        frameworkRepository.findById(command.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy khung năng lực"));

        FrameworkVersion version = frameworkVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản khung năng lực"));

        checkValidRequest(command, version);

        List<FrameworkResultBand> bandsToSave = new ArrayList<>();
        for (var bandCmd : command.bands()) {
            String safeCode = StringNormalization.normalizeCode(bandCmd.code());
            String safeLabel = StringNormalization.trimAndCollapseSpaces(bandCmd.label());
            bandsToSave.add(new FrameworkResultBand(
                    command.versionId(),
                    safeCode,
                    safeLabel,
                    StringNormalization.trimAndCollapseSpaces(bandCmd.description()),
                    bandCmd.order(),
                    now, now, userId, userId));
        }

        try {
            frameworkResultBandRepository.saveAll(bandsToSave);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Mã hoặc nhãn kết quả đã tồn tại cho phiên bản này", e);
        }

        return bandsToSave.stream().map(FrameworkResultBand::getId).collect(Collectors.toList());
    }

    private void checkValidRequest(CreateFrameworkResultBandsCommand command, FrameworkVersion version) {
        if (!version.getFrameworkId().equals(command.frameworkId()))
            throw new IllegalArgumentException("Phiên bản không thuộc khung năng lực này");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalStateException("Chỉ có thể thêm mức kết quả khi phiên bản đang ở trạng thái DRAFT");

        List<FrameworkResultBand> existingBands = frameworkResultBandRepository
            .findByFrameworkVersionId(command.versionId());
        Set<String> existingCodes = existingBands.stream()
            .map(b -> StringNormalization.normalizeCode(b.getCode()))
            .collect(Collectors.toSet());
        Set<String> existingLabels = existingBands.stream()
            .map(b -> StringNormalization.trimAndCollapseSpaces(b.getLabel()))
            .collect(Collectors.toSet());

        Set<String> requestCodes = new HashSet<>();
        Set<String> requestLabels = new HashSet<>();
        for (var bandCmd : command.bands()) {
            String safeCode = StringNormalization.normalizeCode(bandCmd.code());
            String safeLabel = StringNormalization.trimAndCollapseSpaces(bandCmd.label());

            if (!requestCodes.add(safeCode)) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp mã kết quả: " + safeCode);
            }
            if (!requestLabels.add(safeLabel)) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp nhãn kết quả: " + safeLabel);
            }
            if (existingCodes.contains(safeCode)) {
                throw new IllegalArgumentException("Mã kết quả đã tồn tại: " + safeCode);
            }
            if (existingLabels.contains(safeLabel)) {
                throw new IllegalArgumentException("Nhãn kết quả đã tồn tại: " + safeLabel);
            }
        }
    }
}
