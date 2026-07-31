package com.sep.vox.application.port.input.usecase.questionbank;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateQuestionBankGradeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionBankGradeDto;
import com.sep.vox.domain.mapper.QuestionBankGradeDtoMapper;
import com.sep.vox.domain.model.question.QuestionBankGrade;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.repository.QuestionBankGradeRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class CreateQuestionBankGradeUseCase implements IUseCase<CreateQuestionBankGradeCommand, QuestionBankGradeDto> {

    private final QuestionBankRepository questionBankRepository;
    private final QuestionBankGradeRepository questionBankGradeRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public CreateQuestionBankGradeUseCase(
            QuestionBankRepository questionBankRepository,
            QuestionBankGradeRepository questionBankGradeRepository,
            SchoolGradeRepository schoolGradeRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.questionBankRepository = questionBankRepository;
        this.questionBankGradeRepository = questionBankGradeRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public QuestionBankGradeDto execute(CreateQuestionBankGradeCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);

        var questionBank = questionBankRepository.findById(input.questionBankId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        validateAccess(questionBank.getOwnerType(), questionBank.getSchoolId(), currentSchoolId);

        schoolGradeRepository.findById(input.schoolGradeId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy khối lớp"));

        if (questionBankGradeRepository.existsByQuestionBankIdAndSchoolGradeId(questionBank.getId(), input.schoolGradeId())) {
            throw new DuplicatedException("Khối lớp này đã được gắn với ngân hàng câu hỏi");
        }

        var grade = new QuestionBankGrade(
            questionBank.getId(),
            input.schoolGradeId(),
            Instant.now(),
            currentUserId
        );
        var saved = questionBankGradeRepository.save(grade);
        return QuestionBankGradeDtoMapper.toDto(saved);
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
