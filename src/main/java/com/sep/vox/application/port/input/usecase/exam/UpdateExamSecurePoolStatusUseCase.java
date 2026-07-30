package com.sep.vox.application.port.input.usecase.exam;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamSecurePoolStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamSecurePoolDto;
import com.sep.vox.domain.mapper.ExamSecurePoolDtoMapper;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSecurePoolReleaseMode;
import com.sep.vox.domain.model.exam.ExamSecurePoolStatus;
import com.sep.vox.domain.model.question.QuestionConfidentiality;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamSecurePoolRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class UpdateExamSecurePoolStatusUseCase implements IUseCase<UpdateExamSecurePoolStatusCommand, ExamSecurePoolDto> {

    private final ExamSecurePoolRepository examSecurePoolRepository;
    private final ExamMemberRepository examMemberRepository;
    private final QuestionRepository questionRepository;
    private final UserContextPort userContextPort;

    public UpdateExamSecurePoolStatusUseCase(
            ExamSecurePoolRepository examSecurePoolRepository,
            ExamMemberRepository examMemberRepository,
            QuestionRepository questionRepository,
            UserContextPort userContextPort) {
        this.examSecurePoolRepository = examSecurePoolRepository;
        this.examMemberRepository = examMemberRepository;
        this.questionRepository = questionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamSecurePoolDto execute(UpdateExamSecurePoolStatusCommand input) {
        var action = StringNormalization.normalizeCode(input.action());
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var pool = examSecurePoolRepository.findByExamId(input.examId())
            .orElseThrow(() -> new NotFoundException("Bài kiểm tra chưa có câu hỏi nào cần giữ kín"));

        if (!examMemberRepository.existsByExamIdAndUserIdAndRole(input.examId(), currentUserId, ExamMemberRole.CHAIR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (!"RELEASE".equals(action)) {
            throw new IllegalStateException("Action không hợp lệ");
        }
        if (pool.getReleaseMode() != ExamSecurePoolReleaseMode.MANUAL || pool.getStatus() != ExamSecurePoolStatus.SEALED) {
            throw new IllegalStateException("Secure pool không ở trạng thái có thể release thủ công");
        }

        var now = Instant.now();
        pool.setStatus(ExamSecurePoolStatus.RELEASED);
        pool.setReleasedAt(now);
        pool.setReleasedBy(currentUserId);
        pool.setUpdatedAt(now);
        pool.setUpdatedBy(currentUserId);
        var savedPool = examSecurePoolRepository.save(pool);

        for (var question : questionRepository.findBySecurePoolId(savedPool.getId())) {
            question.setConfidentiality(QuestionConfidentiality.RELEASED);
            question.setUpdatedAt(now);
            question.setUpdatedBy(currentUserId);
            questionRepository.save(question);
        }

        return ExamSecurePoolDtoMapper.toDto(savedPool);
    }
}
