package com.sep.vox.application.port.input.usecase.framework;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteFrameworkResultBandCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class DeleteFrameworkResultBandUseCase
        implements IUseCase<DeleteFrameworkResultBandCommand, Void> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;

    public DeleteFrameworkResultBandUseCase(
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            FrameworkCriterionBandRepository frameworkCriterionBandRepository) {
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.frameworkCriterionBandRepository = frameworkCriterionBandRepository;
    }

    @Override
    @Transactional
    public Void execute(DeleteFrameworkResultBandCommand command) {
        FrameworkVersion version = getVersion(command);
        FrameworkResultBand band = getBand(command);

        checkValidRequest(command, version, band);

        frameworkResultBandRepository.deleteById(command.bandId());
        return null;
    }

    private FrameworkVersion getVersion(DeleteFrameworkResultBandCommand command) {
        return frameworkVersionRepository.findFrameworkVersionById(command.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản khung năng lực"));
    }

    private FrameworkResultBand getBand(DeleteFrameworkResultBandCommand command) {
        return frameworkResultBandRepository.findById(command.bandId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy mức kết quả"));
    }

    private void checkValidRequest(DeleteFrameworkResultBandCommand command, FrameworkVersion version, FrameworkResultBand band) {
        if (!version.getFrameworkId().equals(command.frameworkId()))
            throw new IllegalArgumentException("Phiên bản không thuộc framework này");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalStateException("Chỉ có thể chỉnh sửa phiên bản ở trạng thái DRAFT");

        if (!band.getFrameworkVersionId().equals(command.versionId()))
            throw new IllegalArgumentException("Mức kết quả không thuộc phiên bản này");

        if (frameworkCriterionBandRepository.existsByFrameworkResultBandId(command.bandId()))
            throw new IllegalStateException("Không thể xóa mức kết quả đang được sử dụng bởi tiêu chí");
    }
}
