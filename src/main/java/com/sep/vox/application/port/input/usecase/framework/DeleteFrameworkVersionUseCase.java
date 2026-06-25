package com.sep.vox.application.port.input.usecase.framework;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class DeleteFrameworkVersionUseCase implements IUseCase<DeleteFrameworkVersionCommand, Void> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;

    public DeleteFrameworkVersionUseCase(
            FrameworkRepository frameworkRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkCriterionBandRepository frameworkCriterionBandRepository,
            FrameworkResultBandRepository frameworkResultBandRepository) {
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkCriterionBandRepository = frameworkCriterionBandRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
    }

    @Override
    @Transactional
    public Void execute(DeleteFrameworkVersionCommand input) {
        frameworkRepository.findById(input.frameworkId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy framework"));

        var version = frameworkVersionRepository.findById(input.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản framework"));

        if (!version.getFrameworkId().equals(input.frameworkId())) {
            throw new IllegalArgumentException("Phiên bản không thuộc framework này");
        }

        if (version.getStatus() == FrameworkVersionStatus.DRAFT) {
            deleteVersionAndChildren(input.versionId());
        } else {
            frameworkVersionRepository.updateStatus(input.versionId(), FrameworkVersionStatus.ARCHIVED);
            if (version.getStatus() == FrameworkVersionStatus.PUBLISHED) {
                frameworkRepository.updateCurrentVersionId(input.frameworkId(), null);
            }
        }

        return null;
    }

    private void deleteVersionAndChildren(java.util.UUID versionId) {
        var criteria = frameworkCriterionRepository.findByFrameworkVersionId(versionId);
        if (!criteria.isEmpty()) {
            frameworkCriterionBandRepository.deleteByFrameworkCriterionIdIn(
                criteria.stream().map(c -> c.getId()).toList());
        }
        frameworkCriterionRepository.deleteByFrameworkVersionId(versionId);
        frameworkResultBandRepository.deleteByFrameworkVersionId(versionId);
        frameworkVersionRepository.deleteById(versionId);
    }
}
