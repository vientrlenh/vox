package com.sep.vox.application.port.input.usecase.questiontopic;

import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.domain.model.question.QuestionBank;
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

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.questiontopic.PreviewQuestionTopicImportFromFileCommand;
import com.sep.vox.application.port.input.service.QuestionBankManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewImportResponse;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;

/**
 * Previews the topic import file into ONE question bank.
 *
 * <p>The target bank is pinned into {@code session.importedEntityId} right here -- the same way
 * {@code PreviewSystemRubricCriterionImportFromFileUseCase} pins rubricVersionId. That way the Excel file does not
 * need to contain the bank id, and it cannot: each row only carries code/name/description.
 */
@Service
@RequiredArgsConstructor
public class PreviewQuestionTopicImportFromFileUseCase
        implements IUseCase<PreviewQuestionTopicImportFromFileCommand, PreviewImportResponse> {

    private static final int SESSION_EXPIRY_DAYS = 1;

    private final FileProcessingPort fileProcessingPort;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserContextPort userContextPort;
    private final QuestionBankManageAccessService questionBankManageAccessService;
    private final QuestionBankRepository questionBankRepository;

    @Override
    @Transactional
    public PreviewImportResponse execute(PreviewQuestionTopicImportFromFileCommand input) {
        validateCommand(input);

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        QuestionBank questionBank = questionBankRepository.findById(input.questionBankId())
            .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        questionBankManageAccessService.requireManageAccess(questionBank, currentUserId);

        ParseImportFileResult parsed = fileProcessingPort.parse(input.file(), ImportType.QUESTION_TOPIC);
        Instant expiresAt = Instant.now().plus(SESSION_EXPIRY_DAYS, ChronoUnit.DAYS);
        ImportSession savedSession = savePreviewedSession(questionBank, input, parsed, expiresAt);
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

    private void validateCommand(PreviewQuestionTopicImportFromFileCommand input) {
        if (input == null || input.file() == null) {
            throw new IllegalArgumentException("The import file must not be empty");
        }
        if (input.questionBankId() == null) {
            throw new IllegalArgumentException("The question bank must not be empty");
        }
    }

    private ImportSession savePreviewedSession(
            QuestionBank questionBank,
            PreviewQuestionTopicImportFromFileCommand input,
            ParseImportFileResult parsed,
            Instant expiresAt) {
        return importSessionRepository.save(ImportSession.previewedForEntity(
            questionBank.getOwnerType() == QuestionBankOwnerType.SCHOOL ? questionBank.getSchoolId() : null,
            ImportType.QUESTION_TOPIC,
            safeFileName(input.file().fileName()),
            jsonSerializationPort.toJson(parsed.originalHeaders()),
            jsonSerializationPort.toJson(parsed.suggestedMapping()),
            parsed.totalRows(),
            expiresAt,
            questionBank.getId()));
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
        return fileName == null || fileName.isBlank() ? "question-topic-import-file" : fileName;
    }
}
