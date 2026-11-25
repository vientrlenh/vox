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
import com.sep.vox.application.port.input.command.questionbank.PreviewQuestionBankImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewImportResponse;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportSession;
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

        UUID schoolId = resolveSessionSchoolId();

        ParseImportFileResult parsed = fileProcessingPort.parse(input.file(), ImportType.QUESTION_BANK);
        Instant expiresAt = Instant.now().plus(SESSION_EXPIRY_DAYS, ChronoUnit.DAYS);
        ImportSession savedSession = savePreviewedSession(schoolId, input, parsed, expiresAt);
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

    private UUID resolveSessionSchoolId() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID schoolId = null;
        if (!userContextPort.isSystemAdmin()) {
            schoolId = schoolUserRepository.findByUserId(currentUserId)
                .map(schoolUser -> schoolUser.getSchoolId())
                .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));
        }
        return schoolId;
    }

    private ImportSession savePreviewedSession(
            UUID schoolId,
            PreviewQuestionBankImportFromFileCommand input,
            ParseImportFileResult parsed,
            Instant expiresAt) {
        return importSessionRepository.save(ImportSession.previewed(
            schoolId,
            ImportType.QUESTION_BANK,
            safeFileName(input.file().fileName()),
            jsonSerializationPort.toJson(parsed.originalHeaders()),
            jsonSerializationPort.toJson(parsed.suggestedMapping()),
            parsed.totalRows(),
            expiresAt));
    }

    private void saveRows(UUID sessionId, List<Map<String, String>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<ImportRow> importRows = new ArrayList<ImportRow>();
        long rowNumber = 1L;
        for (Map<String, String> row : rows) {
            importRows.add(ImportRow.pending(
                sessionId,
                rowNumber,
                jsonSerializationPort.toJson(new LinkedHashMap<>(row))));
            rowNumber++;
        }
        importRowRepository.saveAll(importRows);
    }

    private String safeFileName(String fileName) {
        return fileName == null || fileName.isBlank() ? "question-bank-import-file" : fileName;
    }
}
