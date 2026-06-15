package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.mapper.question.CreateQuestionResponseMapper;
import com.sep.vox.application.port.input.command.CreateSystemQuestionBankQuestionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.question.CreateQuestionResponse;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionScope;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.question.QuestionVisibility;

import com.sep.vox.domain.model.user.UserStatus;

import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class CreateSystemQuestionBankQuestionUseCase implements IUseCase<CreateSystemQuestionBankQuestionCommand, CreateQuestionResponse> {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final UserContextPort userContextPort;

    public CreateSystemQuestionBankQuestionUseCase(
            UserRepository userRepository,
            QuestionRepository questionRepository,
            QuestionTopicRepository questionTopicRepository,
            QuestionBankRepository questionBankRepository,
            UserContextPort userContextPort) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.questionBankRepository = questionBankRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public CreateQuestionResponse execute(CreateSystemQuestionBankQuestionCommand input) {
        var command = normalize(input);

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        userRepository.findByIdAndStatus(currentUserId, UserStatus.ACTIVE)
            .orElseThrow(() -> new UnauthorizedException("Trang thai nguoi dung khong hop le"));

        var questionTopic = getQuestionTopic(command.questionTopicId());
        var questionBank = questionBankRepository.findById(questionTopic.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay ngan hang cau hoi voi ID nay"));
        if (questionBank.getOwnerType() != QuestionBankOwnerType.SYSTEM) {
            throw new ForbiddenException("Quyen truy cap bi tu choi");
        }

        validateResponseDurationRange(command);

        var now = OffsetDateTime.now();
        var question = Question.create(
            command.questionTopicId(),
            command.code(),
            command.instructionText(),
            command.questionText(),
            command.promptText(),
            command.preparationText(),
            QuestionType.valueOf(command.type()),
            command.preparationTimeSeconds(),
            command.minResponseSeconds(),
            command.maxResponseSeconds(),
            QuestionScope.valueOf(command.scope()),
            QuestionVisibility.valueOf(command.visibility()),
            null,
            false,
            now,
            currentUserId
        );

        var saved = questionRepository.save(question);
        return CreateQuestionResponseMapper.toResponse(saved.getId());
    }

    private CreateSystemQuestionBankQuestionCommand normalize(CreateSystemQuestionBankQuestionCommand input) {
        return new CreateSystemQuestionBankQuestionCommand(
            input.questionTopicId(),
            StringNormalization.normalizeCode(input.code()),
            StringNormalization.trimAndCollapseSpaces(input.instructionText()),
            StringNormalization.trimAndCollapseSpaces(input.questionText()),
            StringNormalization.trimAndCollapseSpaces(input.promptText()),
            StringNormalization.trimAndCollapseSpaces(input.preparationText()),
            StringNormalization.trimAndCollapseSpaces(input.type()),
            StringNormalization.trimAndCollapseSpaces(input.scope()),
            StringNormalization.trimAndCollapseSpaces(input.visibility()),
            input.preparationTimeSeconds(),
            input.minResponseSeconds(),
            input.maxResponseSeconds()
        );
    }

    private QuestionTopic getQuestionTopic(UUID questionTopicId) {
        var questionTopic = questionTopicRepository.findById(questionTopicId)
            .orElseThrow(() -> new NotFoundException("Khong tim thay chu de cau hoi voi ID nay"));

        if (!questionTopic.isActive()) {
            throw new IllegalStateException("Chu de cau hoi yeu cau hien khong hoat dong");
        }
        return questionTopic;
    }

    private void validateResponseDurationRange(CreateSystemQuestionBankQuestionCommand command) {
        if (command.minResponseSeconds() > command.maxResponseSeconds()) {
            throw new IllegalStateException("Thoi gian tra loi toi thieu khong duoc lon hon thoi gian tra loi toi da");
        }
    }

}
