package com.sep.vox.application.port.input.usecase.questionbank;

import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionTopic;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.questionbank.DeleteQuestionBankCommand;
import com.sep.vox.application.port.input.service.QuestionTopicContentDeletionService;
import com.sep.vox.application.port.input.service.QuestionBankManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.questionbank.DeleteQuestionBankResponse;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.repository.QuestionBankGradeRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
@RequiredArgsConstructor
public class DeleteQuestionBankUseCase implements IUseCase<DeleteQuestionBankCommand, DeleteQuestionBankResponse> {

    private final QuestionBankRepository questionBankRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionRepository questionRepository;
    private final QuestionTopicContentDeletionService questionTopicContentDeletionService;
    private final QuestionBankGradeRepository questionBankGradeRepository;
    private final QuestionBankManageAccessService questionBankManageAccessService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public DeleteQuestionBankResponse execute(DeleteQuestionBankCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        QuestionBank questionBank = questionBankRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        questionBankManageAccessService.requireManageAccess(questionBank, currentUserId);

        if (questionRepository.existsPublishedAndUsedByQuestionBankId(questionBank.getId())) {
            questionBank.setStatus(QuestionBankStatus.ARCHIVED);
            questionBankRepository.save(questionBank);
            return new DeleteQuestionBankResponse(false, true);
        }

        deleteWithContents(questionBank);
        return new DeleteQuestionBankResponse(true, false);
    }

    private void deleteWithContents(QuestionBank questionBank) {
        List<QuestionTopic> topics = questionTopicRepository.findByQuestionBankId(questionBank.getId());
        for (QuestionTopic topic : topics) {
            questionTopicContentDeletionService.deleteQuestionsOfTopic(topic.getId());
            questionTopicRepository.deleteById(topic.getId());
        }
        questionBankGradeRepository.deleteByQuestionBankId(questionBank.getId());
        questionBankRepository.deleteById(questionBank.getId());
    }

}
