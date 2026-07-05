package com.sep.vox.application.port.input.usecase.framework;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
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
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class CreateFrameworkCriterionBandsUseCase
        implements IUseCase<CreateFrameworkCriterionBandsCommand, List<UUID>> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final UserContextPort userContextPort;

    public CreateFrameworkCriterionBandsUseCase(
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
    public List<UUID> execute(CreateFrameworkCriterionBandsCommand command) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        OffsetDateTime now = OffsetDateTime.now();

        frameworkRepository.findById(command.frameworkId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khung năng lực"));

        FrameworkVersion version = frameworkVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản khung năng lực"));

        FrameworkCriterion criterion = frameworkCriterionRepository.findById(command.criterionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tiêu chí"));

        Map<String, UUID> resultBandCodeToId = frameworkResultBandRepository.findByFrameworkVersionId(command.versionId())
                .stream().collect(Collectors.toMap(frb -> frb.getCode(), frb -> frb.getId()));

        checkValidRequest(command, version, criterion, resultBandCodeToId);

        List<FrameworkCriterionBand> bandsToSave = new ArrayList<>();
        for (var bandCmd : command.bands()) {
            String safeCode = StringNormalization.normalizeCode(bandCmd.resultBandCode());
            UUID resultBandId = resultBandCodeToId.get(safeCode);
            bandsToSave.add(new FrameworkCriterionBand(
                    command.criterionId(),
                    resultBandId,
                    StringNormalization.trimAndCollapseSpaces(bandCmd.descriptor()),
                    bandCmd.positiveSignals(),
                    bandCmd.negativeSignals(),
                    now, now, userId, userId));
        }

        try {
            frameworkCriterionBandRepository.saveAll(bandsToSave);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Mức đánh giá đã tồn tại cho tiêu chí và kết quả này", e);
        }

        return bandsToSave.stream().map(fcb -> fcb.getId()).collect(Collectors.toList());
    }

    private void checkValidRequest(CreateFrameworkCriterionBandsCommand command, FrameworkVersion version, FrameworkCriterion criterion, Map<String, UUID> resultBandCodeToId) {
        if (!version.getFrameworkId().equals(command.frameworkId())) {
            throw new IllegalArgumentException("Phiên bản không thuộc khung năng lực này");
        }
        if (version.getStatus() != FrameworkVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể thêm mức đánh giá khi phiên bản đang ở trạng thái DRAFT");
        }
        if (!criterion.getFrameworkVersionId().equals(command.versionId())) {
            throw new IllegalArgumentException("Tiêu chí không thuộc phiên bản này");
        }

        Set<String> uniqueCodes = new HashSet<>();
        for (var bandCmd : command.bands()) {
            String safeCode = StringNormalization.normalizeCode(bandCmd.resultBandCode());
            if (!uniqueCodes.add(safeCode)) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp mã kết quả: " + safeCode);
            }
            if (!resultBandCodeToId.containsKey(safeCode)) {
                throw new IllegalArgumentException("Không tìm thấy kết quả với mã: " + safeCode);
            }
        }
    }
}
