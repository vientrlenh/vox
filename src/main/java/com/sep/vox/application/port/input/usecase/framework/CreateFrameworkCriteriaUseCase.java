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
import com.sep.vox.application.port.input.command.CreateFrameworkCriteriaCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class CreateFrameworkCriteriaUseCase
        implements IUseCase<CreateFrameworkCriteriaCommand, List<UUID>> {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final UserContextPort userContextPort;

    public CreateFrameworkCriteriaUseCase(
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            UserContextPort userContextPort) {
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public List<UUID> execute(CreateFrameworkCriteriaCommand command) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        OffsetDateTime now = OffsetDateTime.now();

        FrameworkVersion version = getVersion(command);
        validateRequest(command, version);

        List<FrameworkCriterion> criteriaToSave = new ArrayList<>(command.criteria().size());
        for (var criterionCmd : command.criteria()) {
            criteriaToSave.add(new FrameworkCriterion(
                    command.versionId(),
                    StringNormalization.normalizeCode(criterionCmd.code()),
                    StringNormalization.trimAndCollapseSpaces(criterionCmd.name()),
                    StringNormalization.trimAndCollapseSpaces(criterionCmd.description()),
                    criterionCmd.order(),
                    now, now, userId, userId));
        }

        try {
            return frameworkCriterionRepository.saveAll(criteriaToSave)
                    .stream()
                    .map(fc -> fc.getId())
                    .collect(Collectors.toList());
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Mã tiêu chí đã tồn tại trong cơ sở dữ liệu", e);
        }
    }

    private FrameworkVersion getVersion(CreateFrameworkCriteriaCommand command) {
        return frameworkVersionRepository.findFrameworkVersionById(command.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản khung năng lực"));
    }

    private void validateRequest(CreateFrameworkCriteriaCommand command, FrameworkVersion version) {
        if (!version.getFrameworkId().equals(command.frameworkId()))
            throw new IllegalArgumentException("Phiên bản không thuộc khung năng lực này");

        if (version.getStatus() != FrameworkVersionStatus.DRAFT)
            throw new IllegalStateException("Chỉ có thể thêm tiêu chí khi phiên bản đang ở trạng thái DRAFT");

        Set<String> requestCodes = new HashSet<>();
        for (var criterionCmd : command.criteria()) {
            String safeCode = StringNormalization.normalizeCode(criterionCmd.code());
            if (!requestCodes.add(safeCode)) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp mã tiêu chí: " + safeCode);
            }
        }

        if (frameworkCriterionRepository.existsByFrameworkVersionIdAndCodeIn(command.versionId(), requestCodes))
            throw new IllegalStateException("Mã tiêu chí đã tồn tại");
    }
}
