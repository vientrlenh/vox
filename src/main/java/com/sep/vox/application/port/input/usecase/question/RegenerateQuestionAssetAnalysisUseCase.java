package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.Question;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.service.QuestionEditAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.mapper.QuestionAssetDtoMapper;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
@RequiredArgsConstructor
public class RegenerateQuestionAssetAnalysisUseCase implements IUseCase<RegenerateQuestionAssetAnalysisUseCase.Command, QuestionAssetDto> {

    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEditAccessService questionEditAccessService;
    private final QuestionCloneService questionCloneService;
    private final QuestionAssetAnalysisRequestPublisher questionAssetAnalysisRequestPublisher;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public QuestionAssetDto execute(Command input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        Question question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        QuestionAsset sourceAsset = questionEditAccessService.requireAssetOf(question, input.assetId());
        boolean immutable = questionEditAccessService.authorizeRegeneration(question, currentUserId);

        Question targetQuestion = immutable
            ? questionCloneService.cloneAsDraftWithDetails(question, currentUserId)
            : question;
        QuestionAsset targetAsset = immutable
            ? questionCloneService.findClonedAsset(targetQuestion.getId(), sourceAsset.getOrder())
            : sourceAsset;

        resetForAnalysis(targetAsset);
        QuestionAsset saved = questionAssetRepository.save(targetAsset);
        questionAssetAnalysisRequestPublisher.publish(targetQuestion, saved);
        return QuestionAssetDtoMapper.toDto(saved);
    }

    private void resetForAnalysis(QuestionAsset asset) {
        // Explicit user action ("Tạo lại bằng AI") -- always overwrites, regardless of whether
        // the current content was typed by a person or previously written by AI. Clearing the
        // field to blank here is what lets publishIfNeeded/the completed-consumer's blank-check
        // write into it again.
        if (QuestionAssetAnalysisRequestPublisher.supportsAiTranscript(asset.getType())) {
            asset.setTranscript(null);
        }

        if (QuestionAssetAnalysisRequestPublisher.supportsDescription(asset.getType())) {
            asset.setDescription(null);
        }
    }

    public record Command(UUID questionId, UUID assetId) {
    }
}
