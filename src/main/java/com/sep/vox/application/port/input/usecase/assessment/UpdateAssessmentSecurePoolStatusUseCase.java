package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.AssessmentSecurePool;
import com.sep.vox.domain.model.question.Question;
import java.time.Instant;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateAssessmentSecurePoolStatusCommand;
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
public class UpdateAssessmentSecurePoolStatusUseCase implements IUseCase<UpdateAssessmentSecurePoolStatusCommand, AssessmentSecurePoolDto> {

    private final AssessmentSecurePoolRepository assessmentSecurePoolRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final QuestionRepository questionRepository;
    private final UserContextPort userContextPort;

    public UpdateAssessmentSecurePoolStatusUseCase(
            AssessmentSecurePoolRepository assessmentSecurePoolRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            QuestionRepository questionRepository,
            UserContextPort userContextPort) {
        this.assessmentSecurePoolRepository = assessmentSecurePoolRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.questionRepository = questionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentSecurePoolDto execute(UpdateAssessmentSecurePoolStatusCommand input) {
        String action = StringNormalization.normalizeCode(input.action());
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        AssessmentSecurePool pool = assessmentSecurePoolRepository.findByAssessmentId(input.assessmentId())
            .orElseThrow(() -> new NotFoundException("Bài kiểm tra chưa có câu hỏi nào cần giữ kín"));

        if (!assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(input.assessmentId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (!"RELEASE".equals(action)) {
            throw new IllegalStateException("Action không hợp lệ");
        }
        if (pool.getReleaseMode() != AssessmentSecurePoolReleaseMode.MANUAL || pool.getStatus() != AssessmentSecurePoolStatus.SEALED) {
            throw new IllegalStateException("Secure pool không ở trạng thái có thể release thủ công");
        }

        Instant now = Instant.now();
        pool.setStatus(AssessmentSecurePoolStatus.RELEASED);
        pool.setReleasedAt(now);
        pool.setReleasedBy(currentUserId);
        pool.setUpdatedAt(now);
        pool.setUpdatedBy(currentUserId);
        AssessmentSecurePool savedPool = assessmentSecurePoolRepository.save(pool);

        for (Question question : questionRepository.findBySecurePoolId(savedPool.getId())) {
            question.setConfidentiality(QuestionConfidentiality.RELEASED);
            question.setUpdatedAt(now);
            question.setUpdatedBy(currentUserId);
            questionRepository.save(question);
        }

        return AssessmentSecurePoolDtoMapper.toDto(savedPool);
    }
}
