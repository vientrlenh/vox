package com.sep.vox.application.port.input.usecase.framework;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteFrameworkCriterionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class DeleteFrameworkCriterionUseCase
        implements IUseCase<DeleteFrameworkCriterionCommand, Void> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;

    public DeleteFrameworkCriterionUseCase(
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkCriterionBandRepository frameworkCriterionBandRepository) {
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkCriterionBandRepository = frameworkCriterionBandRepository;
    }

    @Override
    @Transactional
    public Void execute(DeleteFrameworkCriterionCommand command) {
        FrameworkVersion version = getVersion(command);
        FrameworkCriterion criterion = getCriterion(command);

        checkValidRequest(command, version, criterion);

        // delete child bands explicitly before the criterion to avoid an FK violation.
        frameworkCriterionBandRepository.deleteByFrameworkCriterionIdIn(List.of(command.criterionId()));
        frameworkCriterionRepository.deleteById(command.criterionId());
        return null;
    }

    private FrameworkVersion getVersion(DeleteFrameworkCriterionCommand command) {
        return frameworkVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản khung năng lực"));
    }

    private FrameworkCriterion getCriterion(DeleteFrameworkCriterionCommand command) {
        return frameworkCriterionRepository.findById(command.criterionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy tiêu chí"));
    }

    private void checkValidRequest(DeleteFrameworkCriterionCommand command, FrameworkVersion version,
            FrameworkCriterion criterion) {
        if (!version.getFrameworkId().equals(command.frameworkId())) {
            throw new IllegalArgumentException("Phiên bản không thuộc khung năng lực này");
        }
        if (version.getStatus() != FrameworkVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể xóa tiêu chí khi phiên bản đang ở trạng thái DRAFT");
        }
        if (!criterion.getFrameworkVersionId().equals(command.versionId())) {
            throw new IllegalArgumentException("Tiêu chí không thuộc phiên bản này");
        }
    }
}
