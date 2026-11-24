package com.sep.vox.application.port.input.usecase.questionbank;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionTopic;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.questionbank.DeleteQuestionBankCommand;
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
@RequiredArgsConstructor
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

    @Override
    @Transactional
    public DeleteQuestionBankResponse execute(DeleteQuestionBankCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);

        QuestionBank questionBank = questionBankRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        validateAccess(questionBank.getOwnerType(), questionBank.getSchoolId(), currentSchoolId);

        if (questionRepository.existsPublishedAndUsedByQuestionBankId(questionBank.getId())) {
            questionBank.setStatus(QuestionBankStatus.ARCHIVED);
            questionBankRepository.save(questionBank);
            return new DeleteQuestionBankResponse(false, true);
        }

        List<QuestionTopic> topics = questionTopicRepository.findByQuestionBankId(questionBank.getId());
        for (QuestionTopic topic : topics) {
            deleteQuestionsOfTopic(topic.getId());
            questionTopicRepository.deleteById(topic.getId());
        }
        questionBankGradeRepository.deleteByQuestionBankId(questionBank.getId());
        questionBankRepository.deleteById(questionBank.getId());
        return new DeleteQuestionBankResponse(true, false);
    }

    private void deleteQuestionsOfTopic(UUID topicId) {
        List<Question> questions = questionRepository.findByQuestionTopicId(topicId);
        for (Question question : questions) {
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
