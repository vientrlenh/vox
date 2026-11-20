package com.sep.vox.application.port.input.usecase.question;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.mapper.QuestionAssetDtoMapper;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class RegenerateQuestionAssetAnalysisUseCase implements IUseCase<RegenerateQuestionAssetAnalysisUseCase.Command, QuestionAssetDto> {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionCloneService questionCloneService;
    private final QuestionAssetAnalysisRequestPublisher questionAssetAnalysisRequestPublisher;
    private final UserContextPort userContextPort;

    public RegenerateQuestionAssetAnalysisUseCase(
            QuestionRepository questionRepository,
            QuestionBankRepository questionBankRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionCloneService questionCloneService,
            QuestionAssetAnalysisRequestPublisher questionAssetAnalysisRequestPublisher,
            UserContextPort userContextPort) {
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionCloneService = questionCloneService;
        this.questionAssetAnalysisRequestPublisher = questionAssetAnalysisRequestPublisher;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public QuestionAssetDto execute(Command input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        var sourceAsset = questionAssetRepository.findById(input.assetId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy tài nguyên câu hỏi"));
        if (!sourceAsset.getQuestionId().equals(question.getId())) {
            throw new ForbiddenException("Tài nguyên không thuộc câu hỏi này");
        }

        var bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        var owner = currentUserId.equals(question.getCreatedBy());
        var editorCollaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId)
            .filter(collaborator -> collaborator.getPermission() == QuestionCollaboratorPermission.CAN_EDIT)
            .isPresent();
        var systemAdminOnSystemBank = userContextPort.isSystemAdmin()
            && bank.getOwnerType() == QuestionBankOwnerType.SYSTEM;
        if (!systemAdminOnSystemBank && !owner && !editorCollaborator) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var immutable = question.getStatus() == QuestionStatus.PUBLISHED
            || question.isLocked()
            || questionRepository.existsUsedInExam(question.getId());
        var targetQuestion = immutable
            ? questionCloneService.cloneAsDraftWithDetails(question, currentUserId)
            : question;
        var targetAsset = immutable
            ? resolveClonedAsset(targetQuestion.getId(), sourceAsset.getOrder())
            : sourceAsset;

        resetForAnalysis(targetAsset);
        var saved = questionAssetRepository.save(targetAsset);
        questionAssetAnalysisRequestPublisher.publish(targetQuestion, saved);
        return QuestionAssetDtoMapper.toDto(saved);
    }

    private QuestionAsset resolveClonedAsset(UUID questionId, int order) {
        return questionAssetRepository.findByQuestionId(questionId).stream()
            .filter(asset -> asset.getOrder() == order)
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy tài nguyên câu hỏi trong bản nháp mới"));
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
