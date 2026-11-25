package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.question.DeleteQuestionAssetCommand;
import com.sep.vox.application.port.input.service.QuestionEditAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
@RequiredArgsConstructor
public class DeleteQuestionAssetUseCase implements IUseCase<DeleteQuestionAssetCommand, Void> {

    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEditAccessService questionEditAccessService;
    private final QuestionCloneService questionCloneService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(DeleteQuestionAssetCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        Question question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        QuestionAsset sourceAsset = questionEditAccessService.requireAssetOf(question, input.assetId());
        boolean immutable = questionEditAccessService.authorizeEditing(question, currentUserId);

        if (immutable) {
            Question clonedQuestion = questionCloneService.cloneAsDraftWithDetails(question, currentUserId);
            QuestionAsset clonedAsset = questionCloneService.findClonedAsset(clonedQuestion.getId(), sourceAsset.getOrder());
            questionAssetRepository.deleteById(clonedAsset.getId());
            return null;
        }

        questionAssetRepository.deleteById(sourceAsset.getId());
        return null;
    }
}
