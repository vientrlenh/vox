package com.sep.vox.application.port.input.usecase.questiontopic;

import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionTopic;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.questiontopic.DeleteQuestionTopicCommand;
import com.sep.vox.application.port.input.service.QuestionTopicContentDeletionService;
import com.sep.vox.application.port.input.service.QuestionBankManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
@RequiredArgsConstructor
public class DeleteQuestionTopicUseCase implements IUseCase<DeleteQuestionTopicCommand, Void> {

    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionRepository questionRepository;
    private final QuestionTopicContentDeletionService questionTopicContentDeletionService;
    private final QuestionBankManageAccessService questionBankManageAccessService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(DeleteQuestionTopicCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        QuestionTopic topic = questionTopicRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("Question topic not found"));
        QuestionBank bank = questionBankRepository.findById(topic.getQuestionBankId())
            .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));

        questionBankManageAccessService.requireManageAccess(bank, currentUserId);

        if (questionRepository.existsPublishedAndUsedByQuestionTopicId(topic.getId())) {
            throw new ResourceDuplicatedException("The topic still has PUBLISHED questions that are used in an assessment paper");
        }

        questionTopicContentDeletionService.deleteQuestionsOfTopic(topic.getId());
        questionTopicRepository.deleteById(topic.getId());
        return null;
    }

}
