package com.sep.vox.application.port.input.usecase.questiontopic;

import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionTopic;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionTopicStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.mapper.QuestionTopicDtoMapper;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class UpdateQuestionTopicStatusUseCase implements IUseCase<UpdateQuestionTopicStatusCommand, QuestionTopicDto> {

    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public QuestionTopicDto execute(UpdateQuestionTopicStatusCommand input) {
        UpdateQuestionTopicStatusCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);

        QuestionTopic topic = questionTopicRepository.findById(command.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề câu hỏi"));
        QuestionBank bank = questionBankRepository.findById(topic.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        validateAccess(bank.getOwnerType(), bank.getSchoolId(), currentSchoolId);

        switch (command.action()) {
            case "PUBLISH" -> {
                if (topic.getStatus() != QuestionTopicStatus.DRAFT) {
                    throw new IllegalStateException("Chỉ được publish chủ đề đang ở trạng thái DRAFT");
                }
                topic.setStatus(QuestionTopicStatus.PUBLISHED);
            }
            case "ARCHIVE" -> topic.setStatus(QuestionTopicStatus.ARCHIVED);
            default -> throw new IllegalStateException("Action không hợp lệ");
        }


        QuestionTopic saved = questionTopicRepository.save(topic);
        return QuestionTopicDtoMapper.toDto(saved);
    }

    private void validateAccess(QuestionBankOwnerType ownerType, java.util.UUID schoolId, java.util.UUID currentSchoolId) {
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

    private UpdateQuestionTopicStatusCommand normalize(UpdateQuestionTopicStatusCommand input) {
        return new UpdateQuestionTopicStatusCommand(
            input.id(),
            StringNormalization.normalizeCode(input.action())
        );
    }
}
