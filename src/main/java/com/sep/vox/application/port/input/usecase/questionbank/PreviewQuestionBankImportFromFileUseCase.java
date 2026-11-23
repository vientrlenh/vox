package com.sep.vox.application.port.input.usecase.questionbank;

import com.sep.vox.application.response.output.ParseImportFileResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.PreviewQuestionBankImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewImportResponse;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Xem trước file import ngân hàng câu hỏi.
 *
 * <p>Phạm vi sở hữu chốt Ở ĐÂY, không nhận từ client: quản trị hệ thống import ngân hàng SYSTEM
 * ({@code schoolId} null), quản trị trường import ngân hàng của CHÍNH trường mình. Ghi vào
 * {@code session.schoolId} để bước commit chạy ngầm sau đó không phải hỏi lại ai — và không có
 * đường nào cho một file tự nâng phạm vi của nó.
 */
@Service
@RequiredArgsConstructor
public class PreviewQuestionBankImportFromFileUseCase
        implements IUseCase<PreviewQuestionBankImportFromFileCommand, PreviewImportResponse> {

    private static final int SESSION_EXPIRY_DAYS = 1;

    private final FileProcessingPort fileProcessingPort;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public PreviewImportResponse execute(PreviewQuestionBankImportFromFileCommand input) {
        if (input == null || input.file() == null) {
            throw new IllegalArgumentException("File import không được để trống");
        }

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID schoolId = null;
        if (!userContextPort.isSystemAdmin()) {
            schoolId = schoolUserRepository.findByUserId(currentUserId)
                .map(schoolUser -> schoolUser.getSchoolId())
                .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));
        }

        ParseImportFileResult parsed = fileProcessingPort.parse(input.file(), ImportType.QUESTION_BANK);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(SESSION_EXPIRY_DAYS, ChronoUnit.DAYS);
        ImportSession savedSession = importSessionRepository.save(ImportSession.builder()
            .schoolId(schoolId)
            .type(ImportType.QUESTION_BANK)
            .fileName(safeFileName(input.file().fileName()))
            .originalHeadersJson(jsonSerializationPort.toJson(parsed.originalHeaders()))
            .suggestedMappingJson(jsonSerializationPort.toJson(parsed.suggestedMapping()))
            .confirmedMappingJson(null)
            .validRows(0L)
            .invalidRows(0L)
            .importedRows(0L)
            .skippedRows(0L)
            .totalRows(parsed.totalRows())
            .failureReason(null)
            .status(ImportSessionStatus.PREVIEWED)
            .importedEntityId(null)
            .expiresAt(expiresAt)
            .claimedAt(null)
            .claimedBy(null)
            .leaseExpiresAt(null)
            .attempts(0)
            .build());
        saveRows(savedSession.getId(), parsed.rows());

        return new PreviewImportResponse(
            savedSession.getId(),
            safeFileName(input.file().fileName()),
            parsed.originalHeaders(),
            parsed.suggestedMapping(),
            parsed.sampleRows(),
            parsed.totalRows(),
            expiresAt.toString()
        );
    }

    private void saveRows(UUID sessionId, List<Map<String, String>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<ImportRow> importRows = new ArrayList<ImportRow>();
        long rowNumber = 1L;
        for (Map<String, String> row : rows) {
            importRows.add(ImportRow.builder()
                .sessionId(sessionId)
                .rowNumber(rowNumber)
                .rawDataJson(jsonSerializationPort.toJson(new LinkedHashMap<>(row)))
                .mappedDataJson(null)
                .errorsJson(null)
                .status(ImportRowStatus.PENDING)
                .build());
            rowNumber++;
        }
        importRowRepository.saveAll(importRows);
    }

    private String safeFileName(String fileName) {
        return fileName == null || fileName.isBlank() ? "question-bank-import-file" : fileName;
    }
}
