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
import com.sep.vox.application.port.input.command.CreateSchoolQuestionBankQuestionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.question.CreateQuestionResponse;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionScope;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.question.QuestionVisibility;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class CreateSchoolQuestionBankQuestionUseCase implements IUseCase<CreateSchoolQuestionBankQuestionCommand, CreateQuestionResponse> {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public CreateSchoolQuestionBankQuestionUseCase(
            UserRepository userRepository,
            QuestionRepository questionRepository,
            QuestionTopicRepository questionTopicRepository,
            QuestionBankRepository questionBankRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.questionBankRepository = questionBankRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @Override
    @Transactional
    public CreateQuestionResponse execute(CreateSchoolQuestionBankQuestionCommand input) {
        var command = normalize(input);

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findByIdAndStatus(currentUserId, UserStatus.ACTIVE)
            .orElseThrow(() -> new UnauthorizedException("Trang thai nguoi dung khong hop le"));
        ensureTeacherRole(currentUserId);

        var schoolId = getSchoolId(currentUser.getId());

        var questionTopic = questionTopicRepository.findById(command.questionTopicId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay chu de cau hoi"));

        if (!questionTopicRepository.isTopicBelongToSchool(questionTopic.getId(), schoolId)) {
            throw new ForbiddenException("Quyen truy cap bi tu choi");
        }

        var bank = questionBankRepository.findById(questionTopic.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay ngan hang cau hoi"));

        if (bank.getStatus() == QuestionBankStatus.ARCHIVED) {
            throw new ForbiddenException("Ngan hang cau hoi da duoc luu tru");
        }
        if (questionTopic.getStatus() == QuestionTopicStatus.ARCHIVED) {
            throw new ForbiddenException("Chu de cau hoi da duoc luu tru");
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

    private CreateSchoolQuestionBankQuestionCommand normalize(CreateSchoolQuestionBankQuestionCommand input) {
        return new CreateSchoolQuestionBankQuestionCommand(
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

    private void validateResponseDurationRange(CreateSchoolQuestionBankQuestionCommand command) {
        if (command.minResponseSeconds() > command.maxResponseSeconds()) {
            throw new IllegalStateException("Thoi gian tra loi toi thieu khong duoc lon hon thoi gian tra loi toi da");
        }
    }

    private UUID getSchoolId(UUID userId) {
        return schoolUserRepository.findByUserId(userId)
            .map(SchoolUser::getSchoolId)
            .orElseThrow(() -> new IllegalStateException("Nguoi dung hien tai khong thuoc truong nao"));
    }

    private void ensureTeacherRole(UUID userId) {
        var isTeacher = userRoleQueryRepository.findByUserIdWithRoleInfo(userId).stream()
            .anyMatch(role -> "TEACHER".equals(role.roleCode()));
        if (!isTeacher) {
            throw new ForbiddenException("Chi giao vien moi duoc tao cau hoi cua truong");
        }
    }
}
