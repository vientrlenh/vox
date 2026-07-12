package com.sep.vox.application.port.input.usecase.question;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

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
public class GetQuestionAssetUploadUrlUseCase implements IUseCase<GetQuestionAssetUploadUrlQuery, QuestionAssetUploadUrlResponse> {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final UserContextPort userContextPort;
    private final StoragePort storagePort;

    public GetQuestionAssetUploadUrlUseCase(
            QuestionRepository questionRepository,
            QuestionBankRepository questionBankRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            UserContextPort userContextPort,
            StoragePort storagePort) {
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.userContextPort = userContextPort;
        this.storagePort = storagePort;
    }

    @Override
    public QuestionAssetUploadUrlResponse execute(GetQuestionAssetUploadUrlQuery input) {
        var question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        var bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var owner = currentUserId.equals(question.getCreatedBy());
        var editorCollaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId)
            .filter(collaborator -> collaborator.getPermission() == QuestionCollaboratorPermission.CAN_EDIT)
            .isPresent();
        var systemAdminOnSystemBank = userContextPort.isSystemAdmin()
            && bank.getOwnerType() == QuestionBankOwnerType.SYSTEM;
        if (!systemAdminOnSystemBank && !owner && !editorCollaborator) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var normalizedContentType = normalizeContentType(input.contentType());
        validateContentType(normalizedContentType);

        var extension = inferExtension(normalizedContentType);
        var key = "question-assets/%s/%s.%s".formatted(question.getId(), UUID.randomUUID(), extension);
        var presigned = storagePort.presignUpload(key, normalizedContentType, TTL);
        return new QuestionAssetUploadUrlResponse(presigned.uploadUrl(), presigned.publicUrl());
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("Content type không được để trống");
        }

        var normalized = contentType.trim().toLowerCase(Locale.ROOT);
        var separatorIndex = normalized.indexOf(';');
        return separatorIndex >= 0
            ? normalized.substring(0, separatorIndex).trim()
            : normalized;
    }

    private static void validateContentType(String contentType) {
        if (!(contentType.startsWith("image/") || contentType.startsWith("video/"))) {
            throw new IllegalArgumentException("Chỉ hỗ trợ upload asset dạng image/* hoặc video/*");
        }
    }

    private static String inferExtension(String contentType) {
        var slashIndex = contentType.indexOf('/');
        var subtype = slashIndex >= 0 ? contentType.substring(slashIndex + 1) : "bin";
        var sanitized = subtype.replaceAll("[^a-z0-9]+", "-");
        return sanitized.isBlank() ? "bin" : sanitized;
    }
}
