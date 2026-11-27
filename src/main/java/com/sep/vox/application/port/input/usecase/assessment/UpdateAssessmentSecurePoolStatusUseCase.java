package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.AssessmentSecurePool;
import com.sep.vox.domain.model.question.Question;
import java.time.Instant;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessment.UpdateAssessmentSecurePoolStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentSecurePoolDto;
import com.sep.vox.domain.mapper.AssessmentSecurePoolDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolStatus;
import com.sep.vox.domain.model.question.QuestionConfidentiality;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentSecurePoolRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentSecurePoolStatusUseCase implements IUseCase<UpdateAssessmentSecurePoolStatusCommand, AssessmentSecurePoolDto> {

    private final AssessmentSecurePoolRepository assessmentSecurePoolRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final QuestionRepository questionRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentSecurePoolDto execute(UpdateAssessmentSecurePoolStatusCommand input) {
        String action = StringNormalization.normalizeCode(input.action());
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        AssessmentSecurePool pool = assessmentSecurePoolRepository.findByAssessmentId(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("The assessment has no question that needs to be kept secret"));

        if (!assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(input.assessmentId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            throw new ForbiddenException("Access denied");
        }
        requireManualReleasePossible(action, pool);

        Instant now = Instant.now();
        pool.setStatus(AssessmentSecurePoolStatus.RELEASED);
        pool.setReleasedAt(now);
        pool.setReleasedBy(currentUserId);
        AssessmentSecurePool savedPool = assessmentSecurePoolRepository.save(pool);

        releaseQuestions(savedPool);

        return AssessmentSecurePoolDtoMapper.toDto(savedPool);
    }

    private void requireManualReleasePossible(String action, AssessmentSecurePool pool) {
        if (!"RELEASE".equals(action)) {
            throw new IllegalArgumentException("Invalid action");
        }
        if (pool.getReleaseMode() != AssessmentSecurePoolReleaseMode.MANUAL || pool.getStatus() != AssessmentSecurePoolStatus.SEALED) {
            throw new IllegalArgumentException("The secure pool is not in a state that can be released manually");
        }
    }

    private void releaseQuestions(AssessmentSecurePool savedPool) {
        for (Question question : questionRepository.findBySecurePoolId(savedPool.getId())) {
            question.setConfidentiality(QuestionConfidentiality.RELEASED);
            questionRepository.save(question);
        }
    }
}
