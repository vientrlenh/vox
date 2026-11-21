package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionStatusCommand;
import com.sep.vox.application.port.input.service.QuestionStatusActorResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.service.question.QuestionStatusTransition;

/**
 * Cập nhật trạng thái một câu hỏi.
 *
 * <p>Quy tắc chuyển trạng thái nằm ở {@link QuestionStatusTransition} và dùng chung với đường đi
 * hàng loạt, nên hai endpoint không thể lệch nhau. Ở đây lý do từ chối được dịch ngược thành
 * exception để giữ nguyên HTTP status của endpoint đơn (404 / 403 / 400).
 */
@Service
public class UpdateQuestionStatusUseCase implements IUseCase<UpdateQuestionStatusCommand, QuestionDto> {

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final QuestionStatusActorResolver questionStatusActorResolver;

    public UpdateQuestionStatusUseCase(
            QuestionRepository questionRepository,
            QuestionBankRepository questionBankRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            QuestionStatusActorResolver questionStatusActorResolver) {
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
        this.questionStatusActorResolver = questionStatusActorResolver;
    }

    @Override
    @Transactional
    public QuestionDto execute(UpdateQuestionStatusCommand input) {
        String action = StringNormalization.normalizeCode(input.action());
        String note = StringNormalization.trimAndCollapseSpaces(input.note());

        QuestionStatusTransition.Actor actor = questionStatusActorResolver.resolve();
        Question question = questionRepository.findById(input.questionId())
            .orElseThrow(() -> new NotFoundException(QuestionStatusTransition.QUESTION_NOT_FOUND));
        QuestionBank bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new NotFoundException(QuestionStatusTransition.QUESTION_BANK_NOT_FOUND));
        boolean editorCollaborator = questionCollaboratorRepository
            .findByQuestionIdAndUserId(question.getId(), actor.userId())
            .filter(collaborator -> collaborator.getPermission() == QuestionCollaboratorPermission.CAN_EDIT)
            .isPresent();

        QuestionStatusTransition.Rejection rejection = QuestionStatusTransition.rejectionFor(question, bank, editorCollaborator, actor, action, note);
        if (rejection != null) {
            // Cùng một lý do từ chối, hai endpoint dịch ra hai kiểu khác nhau: ở đây là exception
            // để giữ HTTP status, còn endpoint hàng loạt gom vào `failed[]`.
            switch (rejection.kind()) {
                case FORBIDDEN -> throw new ForbiddenException(rejection.reason());
                case INVALID_STATE -> throw new IllegalStateException(rejection.reason());
            }
        }

        QuestionStatusTransition.apply(question, action);
        Question saved = questionRepository.save(question);
        return QuestionDtoMapper.toQuestionDto(saved);
    }
}
