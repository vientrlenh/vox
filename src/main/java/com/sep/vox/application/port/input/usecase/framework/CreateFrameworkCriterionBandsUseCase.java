package com.sep.vox.application.port.input.usecase.framework;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateFrameworkCriterionBandsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class CreateFrameworkCriterionBandsUseCase
        implements IUseCase<CreateFrameworkCriterionBandsCommand, List<UUID>> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final UserContextPort userContextPort;

    public CreateFrameworkCriterionBandsUseCase(
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkCriterionBandRepository frameworkCriterionBandRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            UserContextPort userContextPort) {
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkCriterionBandRepository = frameworkCriterionBandRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public List<UUID> execute(CreateFrameworkCriterionBandsCommand command) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        Instant now = Instant.now();

        FrameworkVersion version = getVersion(command);
        FrameworkCriterion criterion = getCriterion(command);

        Set<String> requestedCodes = command.bands().stream()
                .map(b -> StringNormalization.normalizeCode(b.resultBandCode()))
                .collect(Collectors.toSet());

        if (requestedCodes.size() != command.bands().size())
            throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp mã kết quả");

        List<FrameworkResultBand> existingBands = frameworkResultBandRepository.findByFrameworkVersionIdAndCodeIn(command.versionId(), requestedCodes);

        if (existingBands.size() != requestedCodes.size())
            throw new IllegalArgumentException("Một hoặc nhiều mã kết quả không tồn tại trong phiên bản này");

        Map<String, UUID> resultBandCodeToId = existingBands.stream()
                        .collect(Collectors.toMap(frb -> frb.getCode(), frb -> frb.getId()));

        checkValidRequest(command, version, criterion, resultBandCodeToId);

        List<FrameworkCriterionBand> bandsToCreate = command.bands().stream().map(bandCmd -> {
            String safeCode = StringNormalization.normalizeCode(bandCmd.resultBandCode());
            return new FrameworkCriterionBand(
                    command.criterionId(),
                    resultBandCodeToId.get(safeCode),
                    StringNormalization.trimAndCollapseSpaces(bandCmd.descriptor()),
                    bandCmd.positiveSignals(),
                    bandCmd.negativeSignals(),
                    now, now, userId, userId);
            }).collect(Collectors.toList());

        try {
            return frameworkCriterionBandRepository.saveAll(bandsToCreate)
                    .stream()
                    .map(fcb -> fcb.getId())
                    .collect(Collectors.toList());
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Mã kết quả đã được gán mức đánh giá cho tiêu chí này", e);
        }
    }

    private FrameworkVersion getVersion(CreateFrameworkCriterionBandsCommand command) {
        return frameworkVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản khung năng lực"));
    }

    private FrameworkCriterion getCriterion(CreateFrameworkCriterionBandsCommand command) {
        return frameworkCriterionRepository.findById(command.criterionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy tiêu chí"));
    }

    private void checkValidRequest(CreateFrameworkCriterionBandsCommand command, FrameworkVersion version, FrameworkCriterion criterion, Map<String, UUID> resultBandCodeToId) {
        if (!version.getFrameworkId().equals(command.frameworkId()))
            throw new IllegalArgumentException("Phiên bản không thuộc khung năng lực này");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalStateException("Chỉ có thể thêm mức đánh giá khi phiên bản đang ở trạng thái DRAFT");
        
        if (!criterion.getFrameworkVersionId().equals(command.versionId()))
            throw new IllegalArgumentException("Tiêu chí không thuộc phiên bản này");
    }
}
