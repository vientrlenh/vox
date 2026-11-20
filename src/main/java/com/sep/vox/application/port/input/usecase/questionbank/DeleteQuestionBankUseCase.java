package com.sep.vox.application.port.input.usecase.questionbank;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.questionbank.DeleteQuestionBankResponse;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionBankGradeRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class DeleteQuestionBankUseCase implements IUseCase<DeleteQuestionBankCommand, DeleteQuestionBankResponse> {

    private final QuestionBankRepository questionBankRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final QuestionBankGradeRepository questionBankGradeRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public DeleteQuestionBankUseCase(
            QuestionBankRepository questionBankRepository,
            QuestionTopicRepository questionTopicRepository,
            QuestionRepository questionRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            QuestionBankGradeRepository questionBankGradeRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.questionBankRepository = questionBankRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.questionRepository = questionRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.questionBankGradeRepository = questionBankGradeRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public DeleteQuestionBankResponse execute(DeleteQuestionBankCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);

        var questionBank = questionBankRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        validateAccess(questionBank.getOwnerType(), questionBank.getSchoolId(), currentSchoolId);

        if (questionRepository.existsPublishedAndUsedByQuestionBankId(questionBank.getId())) {
            questionBank.setStatus(QuestionBankStatus.ARCHIVED);
            questionBankRepository.save(questionBank);
            return new DeleteQuestionBankResponse(false, true);
        }

        var topics = questionTopicRepository.findByQuestionBankId(questionBank.getId());
        for (var topic : topics) {
            deleteQuestionsOfTopic(topic.getId());
            questionTopicRepository.deleteById(topic.getId());
        }
        questionBankGradeRepository.deleteByQuestionBankId(questionBank.getId());
        questionBankRepository.deleteById(questionBank.getId());
        return new DeleteQuestionBankResponse(true, false);
    }

    private void deleteQuestionsOfTopic(UUID topicId) {
        var questions = questionRepository.findByQuestionTopicId(topicId);
        for (var question : questions) {
            questionAssetRepository.deleteByQuestionId(question.getId());
            questionEvaluationGuideRepository.deleteByQuestionId(question.getId());
            questionCollaboratorRepository.deleteByQuestionId(question.getId());
            questionRepository.deleteById(question.getId());
        }
    }

    private void validateAccess(QuestionBankOwnerType ownerType, UUID schoolId, UUID currentSchoolId) {
        if (ownerType == QuestionBankOwnerType.SYSTEM) {
            if (!userContextPort.isSystemAdmin()) {
                throw new ForbiddenException("Quyền truy cập bị từ chối");
            }
            return;
        }
        if (currentSchoolId == null || !currentSchoolId.equals(schoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }
}
