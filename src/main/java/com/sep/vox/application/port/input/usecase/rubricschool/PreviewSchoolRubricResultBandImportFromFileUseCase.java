package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.PreviewRubricResultBandImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewRubricResultBandImportResponse;
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
public class PreviewSchoolRubricResultBandImportFromFileUseCase implements IUseCase<PreviewRubricResultBandImportCommand, PreviewRubricResultBandImportResponse> {

    private final RubricVersionRepository rubricVersionRepository;
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
    public PreviewRubricResultBandImportResponse execute(PreviewRubricResultBandImportCommand command) {
        if (command.schoolId() == null) {
            throw new IllegalArgumentException("Yêu cầu không hợp lệ: Thiếu mã trường học đối với luồng School Admin.");
        }

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        // Gác cổng bảo mật trường học
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Bạn không thuộc trường học nào để thực hiện chức năng này."));

        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Bạn không có quyền thao tác trên dữ liệu của trường khác.");
        }

        // Kiểm tra an toàn dữ liệu sở hữu
        RubricVersion version = rubricVersionRepository.findById(command.rubricVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản Rubric yêu cầu."));
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc tương ứng."));

        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Bảo mật: Bộ Rubric này không thuộc sở hữu của trường bạn.");
        }

        // Parse file Excel
        ParseImportFileResult parsedResult = fileProcessingPort.parse(command.file(), ImportType.RUBRIC_RESULT_BAND);
        if (parsedResult.rows() == null || parsedResult.rows().isEmpty()) {
            throw new IllegalArgumentException("File tải lên trống hoặc không chứa dữ liệu hợp lệ.");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, ChronoUnit.DAYS);

        // Lưu Session kèm schoolId của trường
        ImportSession session = ImportSession.builder()
            .schoolId(command.schoolId())
            .type(ImportType.RUBRIC_RESULT_BAND)
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
            .importedEntityId(command.rubricVersionId())
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

        return new PreviewRubricResultBandImportResponse(
                generatedSessionId,
                command.file().fileName(),
                parsedResult.originalHeaders(),
                parsedResult.suggestedMapping(),
                parsedResult.sampleRows(),
                parsedResult.totalRows(),
                expiresAt
        );
    }
}