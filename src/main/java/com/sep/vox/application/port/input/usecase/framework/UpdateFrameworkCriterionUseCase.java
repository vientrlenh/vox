package com.sep.vox.application.port.input.usecase.framework;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class UpdateFrameworkCriterionUseCase
        implements IUseCase<UpdateFrameworkCriterionCommand, UUID> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final UserContextPort userContextPort;

    public UpdateFrameworkCriterionUseCase(
            FrameworkRepository frameworkRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            UserContextPort userContextPort) {
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateFrameworkCriterionCommand command) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        OffsetDateTime now = OffsetDateTime.now();

        frameworkRepository.findById(command.frameworkId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khung năng lực"));

        FrameworkVersion version = frameworkVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản khung năng lực"));

        FrameworkCriterion criterion = frameworkCriterionRepository.findById(command.criterionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tiêu chí"));

        checkValidRequest(command, version, criterion);

        criterion.setCode(StringNormalization.normalizeCode(command.code()));
        criterion.setName(StringNormalization.trimAndCollapseSpaces(command.name()));
        criterion.setDescription(StringNormalization.trimAndCollapseSpaces(command.description()));
        criterion.setOrder(command.order());
        criterion.setUpdatedAt(now);
        criterion.setUpdatedBy(userId);

        try {
            frameworkCriterionRepository.save(criterion);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Mã tiêu chí đã tồn tại", e);
        }

        return criterion.getId();
    }

    private void checkValidRequest(UpdateFrameworkCriterionCommand command, FrameworkVersion version,
            FrameworkCriterion criterion) {
        if (!version.getFrameworkId().equals(command.frameworkId())) {
            throw new IllegalArgumentException("Phiên bản không thuộc khung năng lực này");
        }
        if (version.getStatus() != FrameworkVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể cập nhật tiêu chí khi phiên bản đang ở trạng thái DRAFT");
        }
        if (!criterion.getFrameworkVersionId().equals(command.versionId())) {
            throw new IllegalArgumentException("Tiêu chí không thuộc phiên bản này");
        }

        String safeCode = StringNormalization.normalizeCode(command.code());
        List<FrameworkCriterion> siblings = frameworkCriterionRepository.findByFrameworkVersionId(command.versionId());
        for (FrameworkCriterion other : siblings) {
            if (other.getId().equals(criterion.getId())) {
                continue;
            }
            if (StringNormalization.normalizeCode(other.getCode()).equals(safeCode)) {
                throw new IllegalArgumentException("Mã tiêu chí đã tồn tại: " + safeCode);
            }
        }
    }
}
