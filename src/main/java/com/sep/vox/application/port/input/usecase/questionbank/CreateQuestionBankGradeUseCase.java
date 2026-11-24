package com.sep.vox.application.port.input.usecase.questionbank;

import com.sep.vox.domain.model.question.QuestionBank;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.questionbank.CreateQuestionBankGradeCommand;
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
@RequiredArgsConstructor
public class CreateQuestionBankGradeUseCase implements IUseCase<CreateQuestionBankGradeCommand, QuestionBankGradeDto> {

    private final QuestionBankRepository questionBankRepository;
    private final QuestionBankGradeRepository questionBankGradeRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public QuestionBankGradeDto execute(CreateQuestionBankGradeCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);

        QuestionBank questionBank = questionBankRepository.findById(input.questionBankId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngân hàng câu hỏi"));

        validateAccess(questionBank.getOwnerType(), questionBank.getSchoolId(), currentSchoolId);

        schoolGradeRepository.findById(input.schoolGradeId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khối lớp"));

        if (questionBankGradeRepository.existsByQuestionBankIdAndSchoolGradeId(questionBank.getId(), input.schoolGradeId())) {
            throw new ResourceDuplicatedException("Khối lớp này đã được gắn với ngân hàng câu hỏi");
        }

        QuestionBankGrade grade = QuestionBankGrade.builder()
                .questionBankId(questionBank.getId())
                .schoolGradeId(input.schoolGradeId())
                .attachedAt(Instant.now())
                .attachedBy(currentUserId)
                .build();
        QuestionBankGrade saved = questionBankGradeRepository.save(grade);
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
