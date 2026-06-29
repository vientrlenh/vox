package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateQuestionCollaboratorCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.QuestionCollaboratorDto;
import com.sep.vox.domain.mapper.QuestionCollaboratorDtoMapper;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionCollaborator;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class CreateQuestionCollaboratorUseCase implements IUseCase<CreateQuestionCollaboratorCommand, QuestionCollaboratorDto> {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public CreateQuestionCollaboratorUseCase(
            QuestionRepository questionRepository,
            QuestionBankRepository questionBankRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public QuestionCollaboratorDto execute(CreateQuestionCollaboratorCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var question = questionRepository.findById(command.questionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        var bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        validateOwner(question.getCreatedBy(), currentUserId);
        validateSchoolQuestion(bank.getOwnerType());

        if (currentUserId.equals(command.userId())) {
            throw new IllegalStateException("Không thể thêm chính mình làm collaborator");
        }

        var schoolId = bank.getSchoolId();
        if (schoolId == null || !schoolUserRepository.existsBySchoolIdAndUserId(schoolId, command.userId())) {
            throw new ForbiddenException("Người cộng tác phải thuộc cùng trường");
        }

        var isTeacher = userRoleQueryRepository.findByUserIdWithRoleInfo(command.userId()).stream()
            .anyMatch(role -> "TEACHER".equals(role.roleCode()));
        if (!isTeacher) {
            throw new IllegalStateException("Chỉ có thể chia sẻ câu hỏi cho giáo viên");
        }

        if (questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), command.userId()).isPresent()) {
            throw new IllegalStateException("Người dùng này đã là collaborator của câu hỏi");
        }

        var collaborator = new QuestionCollaborator();
        collaborator.setQuestionId(question.getId());
        collaborator.setUserId(command.userId());
        collaborator.setPermission(QuestionCollaboratorPermission.valueOf(command.permission()));
        collaborator.setAssignedAt(OffsetDateTime.now());

        var saved = questionCollaboratorRepository.save(collaborator);
        return QuestionCollaboratorDtoMapper.toDto(saved);
    }

    private void validateOwner(java.util.UUID ownerId, java.util.UUID currentUserId) {
        if (!currentUserId.equals(ownerId)) {
            throw new ForbiddenException("Chỉ owner mới được quản lý collaborator");
        }
    }

    private void validateSchoolQuestion(QuestionBankOwnerType ownerType) {
        if (ownerType != QuestionBankOwnerType.SCHOOL) {
            throw new IllegalStateException("Chỉ question thuộc school bank mới dùng collaborator");
        }
    }

    private CreateQuestionCollaboratorCommand normalize(CreateQuestionCollaboratorCommand input) {
        return new CreateQuestionCollaboratorCommand(
            input.questionId(),
            input.userId(),
            StringNormalization.normalizeCode(input.permission())
        );
    }
}
