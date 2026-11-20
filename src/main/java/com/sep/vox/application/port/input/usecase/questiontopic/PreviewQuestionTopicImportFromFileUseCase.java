package com.sep.vox.application.port.input.usecase.questiontopic;

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
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.PreviewQuestionTopicImportFromFileCommand;
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
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Xem trước file import chủ đề vào MỘT ngân hàng câu hỏi.
 *
 * <p>Ngân hàng đích ghim vào {@code session.importedEntityId} ngay tại đây — cùng cách
 * {@code PreviewSystemRubricCriterionImportFromFileUseCase} ghim rubricVersionId. Nhờ vậy file
 * Excel không cần chứa id ngân hàng, và cũng không thể chứa: mỗi dòng chỉ mang code/tên/mô tả.
 */
@Service
public class PreviewQuestionTopicImportFromFileUseCase
        implements IUseCase<PreviewQuestionTopicImportFromFileCommand, PreviewImportResponse> {

    private static final int SESSION_EXPIRY_DAYS = 1;

    private final FileProcessingPort fileProcessingPort;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final QuestionBankRepository questionBankRepository;

    public PreviewQuestionTopicImportFromFileUseCase(
            FileProcessingPort fileProcessingPort,
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            JsonSerializationPort jsonSerializationPort,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            QuestionBankRepository questionBankRepository) {
        this.fileProcessingPort = fileProcessingPort;
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.questionBankRepository = questionBankRepository;
    }

    @Override
    @Transactional
    public PreviewImportResponse execute(PreviewQuestionTopicImportFromFileCommand input) {
        if (input == null || input.file() == null) {
            throw new IllegalArgumentException("File import không được để trống");
        }
        if (input.questionBankId() == null) {
            throw new IllegalArgumentException("Ngân hàng câu hỏi không được để trống");
        }

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var questionBank = questionBankRepository.findById(input.questionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        if (questionBank.getOwnerType() == QuestionBankOwnerType.SYSTEM) {
            if (!userContextPort.isSystemAdmin()) {
                throw new ForbiddenException("Quyền truy cập bị từ chối");
            }
        } else {
            var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
                .map(schoolUser -> schoolUser.getSchoolId())
                .orElse(null);
            if (currentSchoolId == null || !currentSchoolId.equals(questionBank.getSchoolId())) {
                throw new ForbiddenException("Quyền truy cập bị từ chối");
            }
        }

        var parsed = fileProcessingPort.parse(input.file(), ImportType.QUESTION_TOPIC);
        var now = Instant.now();
        var expiresAt = now.plus(SESSION_EXPIRY_DAYS, ChronoUnit.DAYS);
        var savedSession = importSessionRepository.save(new ImportSession(
            questionBank.getOwnerType() == QuestionBankOwnerType.SCHOOL ? questionBank.getSchoolId() : null,
            ImportType.QUESTION_TOPIC,
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
            questionBank.getId(),
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
        return fileName == null || fileName.isBlank() ? "question-topic-import-file" : fileName;
    }
}
