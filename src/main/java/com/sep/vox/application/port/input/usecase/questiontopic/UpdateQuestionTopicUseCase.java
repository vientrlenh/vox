package com.sep.vox.application.port.input.usecase.questiontopic;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionTopicCommand;
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
public class UpdateQuestionTopicUseCase implements IUseCase<UpdateQuestionTopicCommand, QuestionTopicDto> {

    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    public UpdateQuestionTopicUseCase(QuestionTopicRepository questionTopicRepository,
            QuestionBankRepository questionBankRepository, UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository) {
        this.questionTopicRepository = questionTopicRepository;
        this.questionBankRepository = questionBankRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public QuestionTopicDto execute(UpdateQuestionTopicCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);

        var topic = questionTopicRepository.findById(command.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề câu hỏi"));
        var bank = questionBankRepository.findById(topic.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        if (bank.getOwnerType() == QuestionBankOwnerType.SYSTEM) {
            if (!userContextPort.isSystemAdmin()) {
                throw new ForbiddenException("Quyền truy cập bị từ chối");
            }
        } else if (currentSchoolId == null || !currentSchoolId.equals(bank.getSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (topic.getStatus() != QuestionTopicStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được cập nhật chủ đề ở trạng thái DRAFT");
        }

        if (command.name() != null) {
            topic.setName(command.name());
        }
        topic.setDescription(command.description());
        topic.setUpdatedAt(OffsetDateTime.now());
        topic.setUpdatedBy(currentUserId);

        var saved = questionTopicRepository.save(topic);
        return QuestionTopicDtoMapper.toDto(saved);
    }

    private UpdateQuestionTopicCommand normalize(UpdateQuestionTopicCommand input) {
        return new UpdateQuestionTopicCommand(
            input.id(),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
    }
}
