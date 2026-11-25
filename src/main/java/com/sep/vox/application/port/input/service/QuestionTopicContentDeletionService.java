package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;

/**
 * Deletes the questions of a topic together with their assets, evaluation guides and collaborators.
 * Runs inside the transaction of the calling use case.
 */
@Service
@RequiredArgsConstructor
public class QuestionTopicContentDeletionService {

    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;

    public void deleteQuestionsOfTopic(UUID topicId) {
        List<Question> questions = questionRepository.findByQuestionTopicId(topicId);
        for (Question question : questions) {
            questionAssetRepository.deleteByQuestionId(question.getId());
            questionEvaluationGuideRepository.deleteByQuestionId(question.getId());
            questionCollaboratorRepository.deleteByQuestionId(question.getId());
            questionRepository.deleteById(question.getId());
        }
    }
}
