package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.PreviewRubricCriterionImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewRubricCriterionImportResponse;
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class PreviewSystemRubricCriterionImportFromFileUseCase implements IUseCase<PreviewRubricCriterionImportCommand, PreviewRubricCriterionImportResponse> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final FileProcessingPort fileProcessingPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public PreviewSystemRubricCriterionImportFromFileUseCase(
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            FileProcessingPort fileProcessingPort,
            JsonSerializationPort jsonSerializationPort,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.fileProcessingPort = fileProcessingPort;
        this.jsonSerializationPort = jsonSerializationPort;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public PreviewRubricCriterionImportResponse execute(PreviewRubricCriterionImportCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        //  Phân luồng bảo mật School vs System
        if (command.schoolId() != null) {
            SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId).orElseThrow(() -> new ForbiddenException("Không thuộc trường học nào."));
            if (!schoolUser.getSchoolId().equals(command.schoolId())) throw new ForbiddenException("Xâm nhập dữ liệu trường khác bị từ chối.");
        }

        //  Dò ngược từ Version -> Rubric
        RubricVersion version = rubricVersionRepository.findById(command.rubricVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản (Version) yêu cầu."));
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));

        //  Chốt chặn Owner Type
        if (command.schoolId() == null && rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("System Admin không được phép can thiệp vào Rubric của Trường học.");
        }
        if (command.schoolId() != null && (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(command.schoolId()))) {
            throw new ForbiddenException("Bộ Rubric này không thuộc sở hữu của trường bạn.");
        }

        // Bóc tách Excel
        ParseImportFileResult parsedResult = fileProcessingPort.parse(command.file(), ImportType.RUBRIC_CRITERION);
        if (parsedResult.rows() == null || parsedResult.rows().isEmpty()) {
            throw new IllegalArgumentException("File tải lên rỗng hoặc không có dữ liệu hợp lệ.");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, ChronoUnit.DAYS);

        // Tạo Session (Ghim rubricVersionId vào importedEntityId)
        ImportSession session = ImportSession.builder()
            .schoolId(command.schoolId())
            .type(ImportType.RUBRIC_CRITERION)
            .fileName(command.file().fileName())
            .originalHeadersJson(jsonSerializationPort.toJson(parsedResult.originalHeaders()))
            .suggestedMappingJson(jsonSerializationPort.toJson(parsedResult.suggestedMapping()))
            .confirmedMappingJson(null)
            .validRows(0)
            .invalidRows(0)
            .importedRows(0)
            .skippedRows(0)
            .totalRows(parsedResult.rows().size())
            .failureReason(null)
            .status(ImportSessionStatus.PREVIEWED)
            // Load the criteria into this versionId
            .importedEntityId(command.rubricVersionId())
            .expiresAt(expiresAt)
            .claimedAt(null)
            .claimedBy(null)
            .leaseExpiresAt(null)
            .attempts(0)
            .build();

        ImportSession savedSession = importSessionRepository.save(session);
        UUID generatedSessionId = savedSession.getId();

        // Tạo Rows nháp
        List<ImportRow> importRows = IntStream.range(0, parsedResult.rows().size())
                .mapToObj(i -> ImportRow.builder()
                    .sessionId(generatedSessionId)
                    .rowNumber(i + 1)
                    .rawDataJson(jsonSerializationPort.toJson(parsedResult.rows().get(i)))
                    .mappedDataJson(null)
                    .errorsJson(null)
                    .status(ImportRowStatus.PENDING)
                    .build()).toList();

        importRowRepository.saveAll(importRows);

        return new PreviewRubricCriterionImportResponse(
                generatedSessionId, command.file().fileName(), parsedResult.originalHeaders(),
                parsedResult.suggestedMapping(), parsedResult.sampleRows(), parsedResult.totalRows(), expiresAt
        );
    }
}