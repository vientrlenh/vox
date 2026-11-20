package com.sep.vox.application.port.input.usecase.questionbank;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
public class PreviewQuestionBankImportFromFileUseCase
        implements IUseCase<PreviewQuestionBankImportFromFileCommand, PreviewImportResponse> {

    private static final int SESSION_EXPIRY_DAYS = 1;

    private final FileProcessingPort fileProcessingPort;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    public PreviewQuestionBankImportFromFileUseCase(
            FileProcessingPort fileProcessingPort,
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            JsonSerializationPort jsonSerializationPort,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository) {
        this.fileProcessingPort = fileProcessingPort;
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public PreviewImportResponse execute(PreviewQuestionBankImportFromFileCommand input) {
        if (input == null || input.file() == null) {
            throw new IllegalArgumentException("File import không được để trống");
        }

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID schoolId = null;
        if (!userContextPort.isSystemAdmin()) {
            schoolId = schoolUserRepository.findByUserId(currentUserId)
                .map(schoolUser -> schoolUser.getSchoolId())
                .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));
        }

        var parsed = fileProcessingPort.parse(input.file(), ImportType.QUESTION_BANK);
        var now = Instant.now();
        var expiresAt = now.plus(SESSION_EXPIRY_DAYS, ChronoUnit.DAYS);
        var savedSession = importSessionRepository.save(new ImportSession(
            schoolId,
            ImportType.QUESTION_BANK,
            safeFileName(input.file().fileName()),
            jsonSerializationPort.toJson(parsed.originalHeaders()),
            jsonSerializationPort.toJson(parsed.suggestedMapping()),
            null,
            0L,
            0L,
            0L,
            0L,
            parsed.totalRows(),
            null,
            ImportSessionStatus.PREVIEWED,
            null,
            expiresAt,
            null,
            null,
            null,
            0,
            now,
            now,
            currentUserId,
            currentUserId
        ));
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
        var importRows = new ArrayList<ImportRow>();
        long rowNumber = 1L;
        for (var row : rows) {
            importRows.add(new ImportRow(
                sessionId,
                rowNumber,
                jsonSerializationPort.toJson(new LinkedHashMap<>(row)),
                null,
                null,
                ImportRowStatus.PENDING
            ));
            rowNumber++;
        }
        importRowRepository.saveAll(importRows);
    }

    private String safeFileName(String fileName) {
        return fileName == null || fileName.isBlank() ? "question-bank-import-file" : fileName;
    }
}
