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
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

@Service
public class CreateFrameworkCriteriaUseCase
        implements IUseCase<CreateFrameworkCriteriaCommand, List<UUID>> {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final UserContextPort userContextPort;

    public CreateFrameworkCriteriaUseCase(
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
    public List<UUID> execute(CreateFrameworkCriteriaCommand command) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        OffsetDateTime now = OffsetDateTime.now();

        frameworkRepository.findById(command.frameworkId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khung năng lực"));

        FrameworkVersion version = frameworkVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản khung năng lực"));

        checkValidRequest(command, version);

        List<FrameworkCriterion> criteriaToSave = new ArrayList<>();
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
            frameworkCriterionRepository.saveAll(criteriaToSave);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Mã tiêu chí đã tồn tại", e);
        }

        return criteriaToSave.stream().map(FrameworkCriterion::getId).collect(Collectors.toList());
    }

    private void checkValidRequest(CreateFrameworkCriteriaCommand command, FrameworkVersion version) {
        if (!version.getFrameworkId().equals(command.frameworkId())) {
            throw new IllegalArgumentException("Phiên bản không thuộc khung năng lực này");
        }
        if (version.getStatus() != FrameworkVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể thêm tiêu chí khi phiên bản đang ở trạng thái DRAFT");
        }

        Set<String> existingCodes = frameworkCriterionRepository.findByFrameworkVersionId(command.versionId())
                .stream()
                .map(c -> StringNormalization.normalizeCode(c.getCode()))
                .collect(Collectors.toSet());

        Set<String> requestCodes = new HashSet<>();
        for (var criterionCmd : command.criteria()) {
            String safeCode = StringNormalization.normalizeCode(criterionCmd.code());
            if (!requestCodes.add(safeCode)) {
                throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp mã tiêu chí: " + safeCode);
            }
            if (existingCodes.contains(safeCode)) {
                throw new IllegalArgumentException("Mã tiêu chí đã tồn tại: " + safeCode);
            }
        }
    }
}
