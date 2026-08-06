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
import com.sep.vox.application.port.input.command.UpdateFrameworkCriterionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionCode;

@Service
public class UpdateFrameworkCriterionUseCase
        implements IUseCase<UpdateFrameworkCriterionCommand, UUID> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final UserContextPort userContextPort;

    public UpdateFrameworkCriterionUseCase(
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            UserContextPort userContextPort) {
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkCriterionCommand command) {
        FrameworkVersion version = getVersion(command);
        FrameworkCriterion criterion = getCriterion(command);

        checkValidRequest(command, version, criterion);
        updateCriterion(command, criterion);

        try {
            frameworkCriterionRepository.save(criterion);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Mã tiêu chí đã tồn tại", e);
        }

        return criterion.getId();
    }

    private FrameworkVersion getVersion(UpdateFrameworkCriterionCommand command) {
        return frameworkVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản khung năng lực"));
    }

    private FrameworkCriterion getCriterion(UpdateFrameworkCriterionCommand command) {
        return frameworkCriterionRepository.findById(command.criterionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy tiêu chí"));
    }

    private void checkValidRequest(UpdateFrameworkCriterionCommand command, FrameworkVersion version,
            FrameworkCriterion criterion) {
        if (!version.getFrameworkId().equals(command.frameworkId()))
            throw new IllegalArgumentException("Phiên bản không thuộc khung năng lực này");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalStateException("Chỉ có thể cập nhật tiêu chí khi phiên bản đang ở trạng thái DRAFT");

        if (!criterion.getFrameworkVersionId().equals(command.versionId()))
            throw new IllegalArgumentException("Tiêu chí không thuộc phiên bản này");

        String safeCode = StringNormalization.normalizeCode(command.code());
        if (!FrameworkCriterionCode.ALLOWED_CODES.contains(safeCode)) {
            throw new IllegalArgumentException("Mã tiêu chí không hợp lệ: " + safeCode);
        }
        if (frameworkCriterionRepository.existsByFrameworkVersionIdAndCodeAndIdNot(command.versionId(), safeCode, command.criterionId())) {
            throw new IllegalStateException("Mã tiêu chí đã tồn tại");
        }

        Set<Integer> otherOrders = frameworkCriterionRepository.findByFrameworkVersionId(command.versionId())
                .stream()
                .filter(fc -> !fc.getId().equals(command.criterionId()))
                .map(fc -> fc.getOrder())
                .collect(Collectors.toSet());
        if (!otherOrders.add(command.order())) {
            throw new IllegalArgumentException("Thứ tự tiêu chí bị trùng lặp: " + command.order());
        }
        int expectedOrder = otherOrders.size();
        for (int i = 1; i <= expectedOrder; i++) {
            if (!otherOrders.contains(i)) {
                throw new IllegalArgumentException("Thứ tự tiêu chí phải tăng dần liên tục từ 1, không được bỏ số");
            }
        }
    }

    private void updateCriterion(UpdateFrameworkCriterionCommand command, FrameworkCriterion criterion) {
        criterion.setCode(StringNormalization.normalizeCode(command.code()));
        criterion.setName(StringNormalization.trimAndCollapseSpaces(command.name()));
        criterion.setDescription(StringNormalization.trimAndCollapseSpaces(command.description()));
        criterion.setOrder(command.order());
        criterion.setUpdatedAt(Instant.now());
        criterion.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
    }
}
