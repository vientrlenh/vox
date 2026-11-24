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
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
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
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionTopicRepository questionTopicRepository;

    @Override
    @Transactional
    public PreviewQuestionImportResponse execute(PreviewQuestionImportFromFileCommand input) {
        if (input == null || input.file() == null) {
            throw new IllegalArgumentException("File import không được để trống");
        }
        // Bỏ trống CẢ HAI = chế độ hàng loạt: mỗi dòng trong tệp tự khai mã ngân hàng + mã chủ đề,
        // nên một tệp rải được sang nhiều chủ đề. Khai một cái mà thiếu cái kia thì không rõ ý định,
        // chặn luôn thay vì đoán.
        boolean bulkMode = input.questionBankId() == null && input.questionTopicId() == null;
        if (!bulkMode && (input.questionBankId() == null || input.questionTopicId() == null)) {
            throw new IllegalArgumentException(
                "Phải chọn cả ngân hàng câu hỏi và chủ đề, hoặc bỏ trống cả hai để import hàng loạt");
        }

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean teacher = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "TEACHER".equals(role.roleCode()));

        UUID sessionSchoolId;
        if (bulkMode) {
            // Phạm vi chốt Ở ĐÂY theo người đăng nhập, KHÔNG lấy từ tệp: quản trị hệ thống nhập
            // vào ngân hàng SYSTEM, người của trường nhập vào ngân hàng của chính trường mình.
            // QuestionImportCommitHandler đọc lại session.schoolId để giới hạn vùng tra mã ngân
            // hàng, nên một dòng không thể trỏ sang trường khác dù có gõ mã trường khác.
            if (userContextPort.isSystemAdmin()) {
                sessionSchoolId = null;
            } else if (currentSchoolId == null) {
                throw new ForbiddenException("Quyền truy cập bị từ chối");
            } else {
                sessionSchoolId = currentSchoolId;
            }
        } else {
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
            if (questionBank.getOwnerType() == QuestionBankOwnerType.SYSTEM) {
                if (!userContextPort.isSystemAdmin()) {
                    throw new ForbiddenException("Quyền truy cập bị từ chối");
                }
            } else if (!teacher || currentSchoolId == null || !currentSchoolId.equals(questionBank.getSchoolId())) {
                throw new ForbiddenException("Quyền truy cập bị từ chối");
            }
            sessionSchoolId = questionBank.getOwnerType() == QuestionBankOwnerType.SCHOOL
                ? questionBank.getSchoolId()
                : null;
        }

        ParseImportFileResult parsed = fileProcessingPort.parse(input.file(), ImportType.QUESTION);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(SESSION_EXPIRY_DAYS, ChronoUnit.DAYS);
        ImportSession savedSession = importSessionRepository.save(ImportSession.previewed(
            sessionSchoolId,
            ImportType.QUESTION,
            safeFileName(input.file().fileName()),
            jsonSerializationPort.toJson(parsed.originalHeaders()),
            jsonSerializationPort.toJson(parsed.suggestedMapping()),
            parsed.totalRows(),
            expiresAt));
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
        List<ImportRow> importRows = new ArrayList<ImportRow>();
        long rowNumber = 1L;
        for (Map<String, String> row : rows) {
            Map<String, String> rawData = new LinkedHashMap<String, String>(row);
            // Ghim ĐỘC LẬP từng cái: chọn được ngân hàng mà chưa chọn chủ đề thì vẫn ghim ngân
            // hàng, phần còn lại QuestionImportCommitHandler lấy theo mã trong file. Trước đây chỉ
            // ghim khi có ĐỦ cả hai, nên "chọn ngân hàng, để trống chủ đề" rơi hết về chế độ hàng
            // loạt và bắt file khai lại cả mã ngân hàng vừa chọn ngay trên màn hình.
            //
            // Để trống cả hai vẫn là chế độ hàng loạt như cũ: mỗi dòng tự khai đủ hai mã.
            if (questionBankId != null) {
                rawData.put("questionBankId", questionBankId.toString());
            }
            if (questionTopicId != null) {
                rawData.put("questionTopicId", questionTopicId.toString());
            }
            importRows.add(ImportRow.pending(sessionId, rowNumber, jsonSerializationPort.toJson(rawData)));
            rowNumber++;
        }
        importRowRepository.saveAll(importRows);
    }

    private String safeFileName(String fileName) {
        return fileName == null || fileName.isBlank() ? "question-import-file" : fileName;
    }
}
