package com.sep.vox.application.port.input.usecase.questionbank;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.QuestionBankPermissionQuery;
import com.sep.vox.application.response.input.questionbank.UpdateQuestionBankResponse;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
public class DeleteQuestionBankUseCase implements IUseCase<DeleteQuestionBankCommand, UpdateQuestionBankResponse> {

    private final QuestionBankRepository questionBankRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionBankPermissionQuery permissionQuery;

    public DeleteQuestionBankUseCase(
            QuestionBankRepository questionBankRepository,
            QuestionTopicRepository questionTopicRepository,
            QuestionRepository questionRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionBankPermissionQuery permissionQuery) {
        this.questionBankRepository = questionBankRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.questionRepository = questionRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.permissionQuery = permissionQuery;
    }

    @Override
    @Transactional
    public UpdateQuestionBankResponse execute(DeleteQuestionBankCommand input) {
        if (!permissionQuery.canUpdateBank(input.bankId())) {
            throw new ForbiddenException("Khong co quyen xoa question bank");
        }

        var bank = questionBankRepository.findById(input.bankId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay ngan hang cau hoi"));

        if (bank.getStatus() != QuestionBankStatus.DRAFT) {
            throw new ForbiddenException("Chi duoc xoa question bank khi dang o DRAFT");
        }

        for (var topic : questionTopicRepository.findByQuestionBankId(input.bankId())) {
            for (var question : questionRepository.findByTopicId(topic.getId())) {
                questionAssetRepository.deleteByQuestionId(question.getId());
                questionEvaluationGuideRepository.deleteByQuestionId(question.getId());
                questionRepository.deleteById(question.getId());
            }
            questionTopicRepository.deleteById(topic.getId());
        }
        questionBankRepository.deleteById(input.bankId());
        return new UpdateQuestionBankResponse(input.bankId());
    }
}
