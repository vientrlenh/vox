package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionBank;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.question.DeleteQuestionAssetCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
@RequiredArgsConstructor
public class DeleteQuestionAssetUseCase implements IUseCase<DeleteQuestionAssetCommand, Void> {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionCloneService questionCloneService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(DeleteQuestionAssetCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        Question question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        QuestionAsset sourceAsset = questionAssetRepository.findById(input.assetId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài nguyên câu hỏi"));
        if (!sourceAsset.getQuestionId().equals(question.getId())) {
            throw new ForbiddenException("Tài nguyên không thuộc câu hỏi này");
        }

        QuestionBank bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        boolean owner = question.isCreatedBy(currentUserId);
        boolean editorCollaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId)
            .filter(collaborator -> collaborator.getPermission() == QuestionCollaboratorPermission.CAN_EDIT)
            .isPresent();
        boolean systemAdminOnSystemBank = userContextPort.isSystemAdmin()
            && bank.getOwnerType() == QuestionBankOwnerType.SYSTEM;
        if (!systemAdminOnSystemBank && !owner && !editorCollaborator) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        boolean immutable = question.getStatus() == QuestionStatus.PUBLISHED
            || question.isLocked()
            || questionRepository.existsUsedInAssessment(question.getId());
        if (!systemAdminOnSystemBank
                && owner
                && !immutable
                && question.getStatus() != QuestionStatus.DRAFT
                && question.getStatus() != QuestionStatus.REVISION_REQUESTED) {
            throw new ForbiddenException("Chỉ được sửa câu hỏi của mình khi ở trạng thái DRAFT hoặc REVISION_REQUESTED");
        }

        if (immutable) {
            Question clonedQuestion = questionCloneService.cloneAsDraftWithDetails(question, currentUserId);
            QuestionAsset clonedAsset = questionAssetRepository.findByQuestionId(clonedQuestion.getId()).stream()
                .filter(asset -> asset.getOrder() == sourceAsset.getOrder())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài nguyên câu hỏi trong bản nháp mới"));
            questionAssetRepository.deleteById(clonedAsset.getId());
            return null;
        }

        questionAssetRepository.deleteById(sourceAsset.getId());
        return null;
    }
}
