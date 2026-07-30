package com.sep.vox.application.port.input.usecase.question;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.PreviewQuestionImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.importfile.PreviewQuestionImportResponse;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class PreviewQuestionImportFromFileUseCase
        implements IUseCase<PreviewQuestionImportFromFileCommand, PreviewQuestionImportResponse> {

    private static final int SESSION_EXPIRY_DAYS = 1;

    private final FileProcessingPort fileProcessingPort;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionTopicRepository questionTopicRepository;

    public PreviewQuestionImportFromFileUseCase(
            FileProcessingPort fileProcessingPort,
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            JsonSerializationPort jsonSerializationPort,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            QuestionBankRepository questionBankRepository,
            QuestionTopicRepository questionTopicRepository) {
        this.fileProcessingPort = fileProcessingPort;
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionTopicRepository = questionTopicRepository;
    }

    @Override
    @Transactional
    public PreviewQuestionImportResponse execute(PreviewQuestionImportFromFileCommand input) {
        if (input == null || input.file() == null) {
            throw new IllegalArgumentException("File import không được để trống");
        }
        if (input.questionBankId() == null || input.questionTopicId() == null) {
            throw new IllegalArgumentException("Question bank và topic không được để trống");
        }

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var teacher = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "TEACHER".equals(role.roleCode()));

        var questionBank = questionBankRepository.findById(input.questionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));
        var questionTopic = questionTopicRepository.findById(input.questionTopicId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề câu hỏi"));

        if (!questionTopic.getQuestionBankId().equals(questionBank.getId())) {
            throw new IllegalStateException("Chủ đề không thuộc ngân hàng câu hỏi đã chọn");
        }
        if (questionTopic.getStatus() != QuestionTopicStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ có thể import câu hỏi vào chủ đề đang ở trạng thái PUBLISHED");
        }
        if (questionBank.getOwnerType() == QuestionBankOwnerType.SYSTEM) {
            if (!userContextPort.isSystemAdmin()) {
                throw new ForbiddenException("Quyền truy cập bị từ chối");
            }
        } else if (!teacher || currentSchoolId == null || !currentSchoolId.equals(questionBank.getSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var parsed = fileProcessingPort.parse(input.file(), ImportType.QUESTION);
        var now = Instant.now();
        var expiresAt = now.plus(SESSION_EXPIRY_DAYS, ChronoUnit.DAYS);
        var savedSession = importSessionRepository.save(new ImportSession(
            questionBank.getOwnerType() == QuestionBankOwnerType.SCHOOL ? questionBank.getSchoolId() : null,
            ImportType.QUESTION,
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

    private void saveRows(
            UUID sessionId,
            java.util.List<Map<String, String>> rows,
            UUID questionBankId,
            UUID questionTopicId) {
        if (rows.isEmpty()) {
            return;
        }
        var importRows = new ArrayList<ImportRow>();
        long rowNumber = 1L;
        for (var row : rows) {
            var rawData = new LinkedHashMap<String, String>(row);
            rawData.put("questionBankId", questionBankId.toString());
            rawData.put("questionTopicId", questionTopicId.toString());
            importRows.add(new ImportRow(
                sessionId,
                rowNumber,
                jsonSerializationPort.toJson(rawData),
                null,
                null,
                ImportRowStatus.PENDING
            ));
            rowNumber++;
        }
        importRowRepository.saveAll(importRows);
    }

    private String safeFileName(String fileName) {
        return fileName == null || fileName.isBlank() ? "question-import-file" : fileName;
    }
}
