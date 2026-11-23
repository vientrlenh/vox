package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.GetQuestionAssetUploadUrlQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.StoragePort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.question.QuestionAssetUploadUrlResponse;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
@RequiredArgsConstructor
public class GetQuestionAssetUploadUrlUseCase implements IUseCase<GetQuestionAssetUploadUrlQuery, QuestionAssetUploadUrlResponse> {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final UserContextPort userContextPort;
    private final StoragePort storagePort;

    @Override
    public QuestionAssetUploadUrlResponse execute(GetQuestionAssetUploadUrlQuery input) {
        Question question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        QuestionBank bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        boolean owner = question.isCreatedBy(currentUserId);
        boolean editorCollaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId)
            .filter(collaborator -> collaborator.getPermission() == QuestionCollaboratorPermission.CAN_EDIT)
            .isPresent();
        boolean systemAdminOnSystemBank = userContextPort.isSystemAdmin()
            && bank.getOwnerType() == QuestionBankOwnerType.SYSTEM;
        if (!systemAdminOnSystemBank && !owner && !editorCollaborator) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        String normalizedContentType = normalizeContentType(input.contentType());
        validateContentType(normalizedContentType);

        String extension = inferExtension(normalizedContentType);
        String key = "question-assets/%s/%s.%s".formatted(question.getId(), UUID.randomUUID(), extension);
        StoragePort.PresignedUpload presigned = storagePort.presignUpload(key, normalizedContentType, TTL);
        return new QuestionAssetUploadUrlResponse(presigned.uploadUrl(), presigned.publicUrl());
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("Content type không được để trống");
        }

        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        int separatorIndex = normalized.indexOf(';');
        return separatorIndex >= 0
            ? normalized.substring(0, separatorIndex).trim()
            : normalized;
    }

    private static void validateContentType(String contentType) {
        if (!(contentType.startsWith("image/")
                || contentType.startsWith("video/")
                || contentType.startsWith("audio/"))) {
            throw new IllegalArgumentException("Chỉ hỗ trợ upload asset dạng image/*, video/* hoặc audio/*");
        }
    }

    private static String inferExtension(String contentType) {
        int slashIndex = contentType.indexOf('/');
        String subtype = slashIndex >= 0 ? contentType.substring(slashIndex + 1) : "bin";
        String sanitized = subtype.replaceAll("[^a-z0-9]+", "-");
        return sanitized.isBlank() ? "bin" : sanitized;
    }
}
