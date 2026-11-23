package com.sep.vox.application.port.input.usecase.questiontopic;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionTopic;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.DeleteQuestionTopicCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class DeleteQuestionTopicUseCase implements IUseCase<DeleteQuestionTopicCommand, Void> {

    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(DeleteQuestionTopicCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);

        QuestionTopic topic = questionTopicRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chủ đề câu hỏi"));
        QuestionBank bank = questionBankRepository.findById(topic.getQuestionBankId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        validateAccess(bank.getOwnerType(), bank.getSchoolId(), currentSchoolId);

        if (questionRepository.existsPublishedAndUsedByQuestionTopicId(topic.getId())) {
            throw new ResourceDuplicatedException("Chủ đề vẫn còn câu hỏi đã PUBLISHED và đang được dùng trong đề thi");
        }

        List<Question> questions = questionRepository.findByQuestionTopicId(topic.getId());
        for (Question question : questions) {
            questionAssetRepository.deleteByQuestionId(question.getId());
            questionEvaluationGuideRepository.deleteByQuestionId(question.getId());
            questionCollaboratorRepository.deleteByQuestionId(question.getId());
            questionRepository.deleteById(question.getId());
        }
        questionTopicRepository.deleteById(topic.getId());
        return null;
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
