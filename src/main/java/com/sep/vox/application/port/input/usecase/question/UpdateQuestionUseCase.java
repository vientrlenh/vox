package com.sep.vox.application.port.input.usecase.question;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.question.UpdateQuestionResponseMapper;
import com.sep.vox.application.port.input.command.UpdateQuestionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class UpdateQuestionUseCase implements IUseCase<UpdateQuestionCommand, UpdateQuestionResponse> {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final UserContextPort userContextPort;

    public UpdateQuestionUseCase(
            QuestionRepository questionRepository,
            QuestionBankRepository questionBankRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            UserContextPort userContextPort) {
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UpdateQuestionResponse execute(UpdateQuestionCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var question = questionRepository.findById(command.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        var bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        var owner = currentUserId.equals(question.getCreatedBy());
        var editorCollaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId)
            .filter(collaborator -> collaborator.getPermission() == QuestionCollaboratorPermission.CAN_EDIT)
            .isPresent();
        var systemAdminOnSystemBank = userContextPort.isSystemAdmin()
            && bank.getOwnerType() == QuestionBankOwnerType.SYSTEM;

        if (!systemAdminOnSystemBank && !editorCollaborator && !owner) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        // sharing là cài đặt riêng tư, không phải nội dung — được chỉnh bất kỳ lúc nào
        if (command.sharing() != null) {
            question.setSharing(QuestionSharing.valueOf(command.sharing()));
        }

        var hasContentUpdate = command.instructionText() != null || command.questionText() != null
                || command.promptText() != null || command.preparationText() != null
                || command.questionType() != null || command.preparationTimeSeconds() != null
                || command.minResponseSeconds() != null || command.maxResponseSeconds() != null;

        if (hasContentUpdate) {
            var usedInExam = questionRepository.existsUsedInExam(question.getId());
            var immutable = question.getStatus() == QuestionStatus.PUBLISHED || question.isLocked() || usedInExam;
            if (immutable) {
                throw new IllegalStateException(
                    "Câu hỏi đã publish hoặc đã dùng trong bài kiểm tra, không thể sửa trực tiếp — dùng nút Nhân bản để tạo bản có thể sửa");
            }
            if (!systemAdminOnSystemBank
                    && (owner || editorCollaborator)
                    && question.getStatus() != QuestionStatus.DRAFT
                    && question.getStatus() != QuestionStatus.REVISION_REQUESTED) {
                throw new ForbiddenException("Chỉ được sửa câu hỏi khi ở trạng thái DRAFT hoặc REVISION_REQUESTED");
            }
            if (command.preparationTimeSeconds() == null || command.minResponseSeconds() == null || command.maxResponseSeconds() == null) {
                throw new IllegalStateException("Phải nhập đầy đủ thời gian chuẩn bị, thời gian trả lời tối thiểu và tối đa");
            }
            applyContentUpdates(question, command, currentUserId);
        }

        question.setUpdatedAt(Instant.now());
        question.setUpdatedBy(currentUserId);
        questionRepository.save(question);

        return UpdateQuestionResponseMapper.toResponse(question);
    }

    private void applyContentUpdates(Question question, UpdateQuestionCommand command, UUID currentUserId) {
        if (command.instructionText() != null) question.setInstructionText(command.instructionText());
        if (command.questionText() != null) question.setQuestionText(command.questionText());
        if (command.promptText() != null) question.setPromptText(command.promptText());
        if (command.preparationText() != null) question.setPreparationText(command.preparationText());
        if (command.questionType() != null) question.setType(QuestionType.valueOf(command.questionType()));
        if (command.preparationTimeSeconds() != null) question.setPreparationTimeSeconds(command.preparationTimeSeconds());
        if (command.minResponseSeconds() != null) question.setMinResponseSeconds(command.minResponseSeconds());
        if (command.maxResponseSeconds() != null) question.setMaxResponseSeconds(command.maxResponseSeconds());
        if (question.getMinResponseSeconds() > question.getMaxResponseSeconds()) {
            throw new IllegalStateException("Thời gian trả lời tối thiểu không được lớn hơn thời gian trả lời tối đa");
        }
        question.setUpdatedAt(Instant.now());
        question.setUpdatedBy(currentUserId);
    }

    private UpdateQuestionCommand normalize(UpdateQuestionCommand input) {
        return new UpdateQuestionCommand(
            input.id(),
            StringNormalization.trimAndCollapseSpaces(input.instructionText()),
            StringNormalization.trimAndCollapseSpaces(input.questionText()),
            StringNormalization.trimAndCollapseSpaces(input.promptText()),
            StringNormalization.trimAndCollapseSpaces(input.preparationText()),
            input.questionType() == null ? null : StringNormalization.normalizeCode(input.questionType()),
            input.preparationTimeSeconds(),
            input.minResponseSeconds(),
            input.maxResponseSeconds(),
            input.sharing() == null ? null : StringNormalization.normalizeCode(input.sharing())
        );
    }
}
