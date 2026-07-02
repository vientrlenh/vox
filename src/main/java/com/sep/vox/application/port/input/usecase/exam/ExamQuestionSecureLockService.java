package com.sep.vox.application.port.input.usecase.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.exam.ExamSecurePool;
import com.sep.vox.domain.model.exam.ExamSecurePoolReleaseMode;
import com.sep.vox.domain.model.exam.ExamSecurePoolStatus;
import com.sep.vox.domain.model.question.QuestionConfidentiality;
import com.sep.vox.domain.repository.ExamSecurePoolRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class ExamQuestionSecureLockService {

    private final ExamSecurePoolRepository examSecurePoolRepository;
    private final QuestionRepository questionRepository;

    public ExamQuestionSecureLockService(
            ExamSecurePoolRepository examSecurePoolRepository,
            QuestionRepository questionRepository) {
        this.examSecurePoolRepository = examSecurePoolRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public void lockQuestionForExam(
            UUID questionId,
            UUID examId,
            ExamSecurePoolReleaseMode releaseModeIfCreating,
            UUID currentUserId) {
        var now = OffsetDateTime.now();
        var pool = examSecurePoolRepository.findByExamId(examId)
            .orElseGet(() -> examSecurePoolRepository.save(new ExamSecurePool(
                examId,
                ExamSecurePoolStatus.SEALED,
                releaseModeIfCreating,
                null,
                null,
                null,
                now,
                now,
                currentUserId,
                currentUserId
            )));

        var question = questionRepository.findById(questionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        question.setLocked(true);
        question.setConfidentiality(QuestionConfidentiality.EXAM_RESTRICTED);
        question.setSecurePoolId(pool.getId());
        question.setUpdatedAt(now);
        question.setUpdatedBy(currentUserId);
        questionRepository.save(question);
    }

    @Transactional
    public void releaseIfAutoAfterClose(UUID examId) {
        var pool = examSecurePoolRepository.findByExamId(examId).orElse(null);
        if (pool == null
                || pool.getReleaseMode() != ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE
                || pool.getStatus() != ExamSecurePoolStatus.SEALED) {
            return;
        }
        var now = OffsetDateTime.now();
        pool.setStatus(ExamSecurePoolStatus.RELEASED);
        pool.setReleasedAt(now);
        pool.setUpdatedAt(now);
        var savedPool = examSecurePoolRepository.save(pool);

        for (var question : questionRepository.findBySecurePoolId(savedPool.getId())) {
            question.setConfidentiality(QuestionConfidentiality.RELEASED);
            question.setUpdatedAt(now);
            questionRepository.save(question);
        }
    }
}
