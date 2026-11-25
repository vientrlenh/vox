package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionTopic;
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
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.question.PreviewQuestionImportFromFileCommand;
import com.sep.vox.application.port.input.service.QuestionEditAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewQuestionImportResponse;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class PreviewQuestionImportFromFileUseCase
        implements IUseCase<PreviewQuestionImportFromFileCommand, PreviewQuestionImportResponse> {

    private static final int SESSION_EXPIRY_DAYS = 1;

    private final FileProcessingPort fileProcessingPort;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final QuestionEditAccessService questionEditAccessService;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionTopicRepository questionTopicRepository;

    @Override
    @Transactional
    public PreviewQuestionImportResponse execute(PreviewQuestionImportFromFileCommand input) {
        boolean bulkMode = requireValidSelection(input);

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID sessionSchoolId = bulkMode
            ? resolveBulkSessionSchoolId(currentUserId)
            : resolveSelectedSessionSchoolId(input, currentUserId);

        ParseImportFileResult parsed = fileProcessingPort.parse(input.file(), ImportType.QUESTION);
        Instant expiresAt = Instant.now().plus(SESSION_EXPIRY_DAYS, ChronoUnit.DAYS);
        ImportSession savedSession = savePreviewedSession(sessionSchoolId, input, parsed, expiresAt);
        saveRows(savedSession.getId(), parsed.rows(), input.questionBankId(), input.questionTopicId());

        return new PreviewQuestionImportResponse(
            savedSession.getId(),
            safeFileName(input.file().fileName()),
            parsed.originalHeaders(),
            parsed.suggestedMapping(),
            parsed.sampleRows(),
            parsed.totalRows(),
            expiresAt.toString()
        );
    }

    /**
     * @return {@code true} for bulk mode
     */
    private boolean requireValidSelection(PreviewQuestionImportFromFileCommand input) {
        if (input == null || input.file() == null) {
            throw new IllegalArgumentException("File import không được để trống");
        }
        // Leaving BOTH empty means bulk mode: every row of the file declares its own bank code and topic code,
        // so one file can spread over several topics. Declaring one without the other has no clear intent,
        // so it is rejected instead of guessed.
        boolean bulkMode = input.questionBankId() == null && input.questionTopicId() == null;
        if (!bulkMode && (input.questionBankId() == null || input.questionTopicId() == null)) {
            throw new IllegalArgumentException(
                "Phải chọn cả ngân hàng câu hỏi và chủ đề, hoặc bỏ trống cả hai để import hàng loạt");
        }
        return bulkMode;
    }

    /**
     * The scope is fixed HERE from the logged-in user, NOT from the file: a system admin imports into the SYSTEM
     * banks, a school member imports into the banks of their own school. QuestionImportCommitHandler re-reads
     * session.schoolId to limit the bank-code lookup, so a row cannot point at another school even if it types
     * another school's code.
     */
    private UUID resolveBulkSessionSchoolId(UUID currentUserId) {
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        if (userContextPort.isSystemAdmin()) {
            return null;
        }
        if (currentSchoolId == null) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        return currentSchoolId;
    }

    private UUID resolveSelectedSessionSchoolId(PreviewQuestionImportFromFileCommand input, UUID currentUserId) {
        QuestionBank questionBank = questionBankRepository.findById(input.questionBankId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngân hàng câu hỏi"));
        QuestionTopic questionTopic = questionTopicRepository.findById(input.questionTopicId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chủ đề câu hỏi"));

        if (!questionTopic.getQuestionBankId().equals(questionBank.getId())) {
            throw new IllegalArgumentException("Chủ đề không thuộc ngân hàng câu hỏi đã chọn");
        }
        if (questionTopic.getStatus() != QuestionTopicStatus.PUBLISHED) {
            throw new IllegalArgumentException("Chỉ có thể import câu hỏi vào chủ đề đang ở trạng thái PUBLISHED");
        }
        questionEditAccessService.requireCreationAccess(questionBank, currentUserId);
        return questionBank.getOwnerType() == QuestionBankOwnerType.SCHOOL
            ? questionBank.getSchoolId()
            : null;
    }

    private ImportSession savePreviewedSession(
            UUID sessionSchoolId,
            PreviewQuestionImportFromFileCommand input,
            ParseImportFileResult parsed,
            Instant expiresAt) {
        return importSessionRepository.save(ImportSession.previewed(
            sessionSchoolId,
            ImportType.QUESTION,
            safeFileName(input.file().fileName()),
            jsonSerializationPort.toJson(parsed.originalHeaders()),
            jsonSerializationPort.toJson(parsed.suggestedMapping()),
            parsed.totalRows(),
            expiresAt));
    }

    private void saveRows(
            UUID sessionId,
            List<Map<String, String>> rows,
            UUID questionBankId,
            UUID questionTopicId) {
        if (rows.isEmpty()) {
            return;
        }
        List<ImportRow> importRows = new ArrayList<ImportRow>();
        long rowNumber = 1L;
        for (Map<String, String> row : rows) {
            Map<String, String> rawData = withPinnedScope(row, questionBankId, questionTopicId);
            importRows.add(ImportRow.pending(sessionId, rowNumber, jsonSerializationPort.toJson(rawData)));
            rowNumber++;
        }
        importRowRepository.saveAll(importRows);
    }

    /**
     * Pins each id INDEPENDENTLY: a bank chosen without a topic is still pinned, and
     * QuestionImportCommitHandler takes the rest from the codes in the file. It used to pin only when BOTH were
     * given, so "bank chosen, topic left empty" fell back to bulk mode and made the file repeat the bank code
     * that was just chosen on screen.
     *
     * <p>Leaving both empty is still bulk mode as before: every row declares both codes itself.
     */
    private Map<String, String> withPinnedScope(Map<String, String> row, UUID questionBankId, UUID questionTopicId) {
        Map<String, String> rawData = new LinkedHashMap<String, String>(row);
        if (questionBankId != null) {
            rawData.put("questionBankId", questionBankId.toString());
        }
        if (questionTopicId != null) {
            rawData.put("questionTopicId", questionTopicId.toString());
        }
        return rawData;
    }

    private String safeFileName(String fileName) {
        return fileName == null || fileName.isBlank() ? "question-import-file" : fileName;
    }
}
