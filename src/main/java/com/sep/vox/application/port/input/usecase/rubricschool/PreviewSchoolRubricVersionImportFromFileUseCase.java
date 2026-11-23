package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.PreviewSchoolRubricVersionImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewRubricVersionImportResponse;
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PreviewSchoolRubricVersionImportFromFileUseCase implements IUseCase<PreviewSchoolRubricVersionImportCommand, PreviewRubricVersionImportResponse> {

    private final RubricRepository rubricRepository;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final FileProcessingPort fileProcessingPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public PreviewRubricVersionImportResponse execute(PreviewSchoolRubricVersionImportCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");

        // Kiểm tra quyền trường học
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId).orElseThrow(() -> new ForbiddenException("Không thuộc trường học nào."));
        if (!schoolUser.getSchoolId().equals(command.schoolId())) throw new ForbiddenException("Hành vi xâm nhập dữ liệu trường học khác bị từ chối.");

        Rubric rubric = rubricRepository.findById(command.rubricId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric yêu cầu."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || rubric.getSchoolId() == null || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Bộ Rubric này không thuộc quyền quản lý của trường bạn.");
        }

        ParseImportFileResult parsedResult = fileProcessingPort.parse(command.file(), ImportType.RUBRIC_VERSION);
        if (parsedResult.rows() == null || parsedResult.rows().isEmpty()) {
            throw new IllegalArgumentException("File tải lên trống hoặc không chứa dữ liệu.");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, ChronoUnit.DAYS);

        // Lưu thông tin kèm trường schoolId đầy đủ
        ImportSession session = ImportSession.builder()
            // Hard-bind the school id to the import session
            .schoolId(command.schoolId())
            .type(ImportType.RUBRIC_VERSION)
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
            .importedEntityId(command.rubricId())
            .expiresAt(expiresAt)
            .claimedAt(null)
            .claimedBy(null)
            .leaseExpiresAt(null)
            .attempts(0)
            .build();

        ImportSession savedSession = importSessionRepository.save(session);
        UUID generatedSessionId = savedSession.getId();

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

        return new PreviewRubricVersionImportResponse(
                generatedSessionId, command.file().fileName(), parsedResult.originalHeaders(),
                parsedResult.suggestedMapping(), parsedResult.sampleRows(), parsedResult.totalRows(), expiresAt
        );
    }
}