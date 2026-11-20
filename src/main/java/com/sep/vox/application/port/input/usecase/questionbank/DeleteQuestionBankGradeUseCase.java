package com.sep.vox.application.port.input.usecase.questionbank;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteQuestionBankGradeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.repository.QuestionBankGradeRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class DeleteQuestionBankGradeUseCase implements IUseCase<DeleteQuestionBankGradeCommand, Void> {

    private final QuestionBankRepository questionBankRepository;
    private final QuestionBankGradeRepository questionBankGradeRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public DeleteQuestionBankGradeUseCase(
            QuestionBankRepository questionBankRepository,
            QuestionBankGradeRepository questionBankGradeRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.questionBankRepository = questionBankRepository;
        this.questionBankGradeRepository = questionBankGradeRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteQuestionBankGradeCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);

        var questionBank = questionBankRepository.findById(input.questionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        validateAccess(questionBank.getOwnerType(), questionBank.getSchoolId(), currentSchoolId);

        var grade = questionBankGradeRepository.findById(input.gradeRowId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy khối lớp đã gắn"));
        if (!grade.getQuestionBankId().equals(questionBank.getId())) {
            throw new NotFoundException("Không tìm thấy khối lớp đã gắn");
        }

        questionBankGradeRepository.deleteById(grade.getId());
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
