package com.sep.vox.application.port.input.usecase.questiontopic;

import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionTopic;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.questiontopic.UpdateQuestionTopicStatusCommand;
import com.sep.vox.application.port.input.service.QuestionBankManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.mapper.QuestionTopicDtoMapper;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
@RequiredArgsConstructor
public class UpdateQuestionTopicStatusUseCase implements IUseCase<UpdateQuestionTopicStatusCommand, QuestionTopicDto> {

    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final UserContextPort userContextPort;
    private final QuestionBankManageAccessService questionBankManageAccessService;

    @Override
    @Transactional
    public QuestionTopicDto execute(UpdateQuestionTopicStatusCommand input) {
        UpdateQuestionTopicStatusCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        QuestionTopic topic = questionTopicRepository.findById(command.id())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chủ đề câu hỏi"));
        QuestionBank bank = questionBankRepository.findById(topic.getQuestionBankId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        questionBankManageAccessService.requireManageAccess(bank, currentUserId);

        applyAction(topic, command.action());

        QuestionTopic saved = questionTopicRepository.save(topic);
        return QuestionTopicDtoMapper.toDto(saved);
    }

    private void applyAction(QuestionTopic topic, String action) {
        switch (action) {
            case "PUBLISH" -> {
                if (topic.getStatus() != QuestionTopicStatus.DRAFT) {
                    throw new IllegalArgumentException("Chỉ được publish chủ đề đang ở trạng thái DRAFT");
                }
                topic.setStatus(QuestionTopicStatus.PUBLISHED);
            }
            case "ARCHIVE" -> topic.setStatus(QuestionTopicStatus.ARCHIVED);
            default -> throw new IllegalArgumentException("Action không hợp lệ");
        }
    }

    private UpdateQuestionTopicStatusCommand normalize(UpdateQuestionTopicStatusCommand input) {
        return new UpdateQuestionTopicStatusCommand(
            input.id(),
            StringNormalization.normalizeCode(input.action())
        );
    }
}
