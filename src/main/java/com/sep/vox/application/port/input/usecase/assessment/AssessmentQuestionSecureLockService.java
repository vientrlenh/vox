package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.AssessmentSecurePool;
import com.sep.vox.domain.model.question.Question;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolStatus;
import com.sep.vox.domain.model.question.QuestionConfidentiality;
import com.sep.vox.domain.repository.AssessmentSecurePoolRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class AssessmentQuestionSecureLockService {

    private final AssessmentSecurePoolRepository assessmentSecurePoolRepository;
    private final QuestionRepository questionRepository;

    public AssessmentQuestionSecureLockService(
            AssessmentSecurePoolRepository assessmentSecurePoolRepository,
            QuestionRepository questionRepository) {
        this.assessmentSecurePoolRepository = assessmentSecurePoolRepository;
        this.questionRepository = questionRepository;
    }

    /**
     * DISABLED ON PURPOSE -- attaching a question to an assessment no longer seals that question.
     *
     * <p>Previously this function built an {@code AssessmentSecurePool} and set {@code locked=true},
     * {@code confidentiality=ASSESSMENT_RESTRICTED} and pointed {@code securePoolId} at the pool. It was dropped because that
     * confidentiality level did NOT protect anything for real: neither {@code findAccessible} nor {@code findAccessibleForAssessmentPaper}
     * filters by {@code confidentiality}, so the question was still visible to everyone and still selectable
     * for another assessment. It only stuck a label on it that said the wrong thing about its real state.
     *
     * <p>It also stops setting {@code locked}: once the pool is gone there is no way left to find the question again and unlock it,
     * so setting the lock would be locking it forever.
     *
     * <p>Nothing is lost by dropping that flag, because a question that wants to get into an assessment or a paper frame MUST be
     * {@code PUBLISHED} ({@code findAccessibleForAssessmentPaper} filters {@code q.status = 'PUBLISHED'}),
     * and {@code PUBLISHED} by itself is already part of the {@code immutable} check on every edit path:
     * {@code status == PUBLISHED || isLocked() || existsUsedInAssessment(...)}. That means the first operand is always
     * true before the other two are even evaluated -- a question inside an assessment still has to be cloned before it can be edited.
     *
     * <p>The signature and the call sites are kept so that re-enabling it only needs reverting the body of this function.
     */
    @Transactional
    public void lockQuestionForAssessment(
            UUID questionId,
            UUID assessmentId,
            AssessmentSecurePoolReleaseMode releaseModeIfCreating,
            UUID currentUserId) {
    }

    @Transactional
    public void unlockQuestion(UUID questionId, UUID currentUserId) {
        Question question = questionRepository.findById(questionId).orElse(null);
        if (question == null || !question.isLocked()) {
            return;
        }
        question.setLocked(false);
        question.setConfidentiality(QuestionConfidentiality.RELEASED);
        question.setSecurePoolId(null);
        questionRepository.save(question);
    }

    /**
     * Xoá hẳn kỳ thi thì kho câu hỏi mật của nó cũng biến mất, nên phải mở khoá mọi câu hỏi đang
     * trỏ vào pool trước rồi mới xoá pool. Bỏ bước này là câu hỏi nằm lại ngân hàng đề ở trạng thái
     * khoá với securePoolId trỏ vào một pool không còn tồn tại — không còn đường nào giải phóng.
     */
    @Transactional
    public void releaseAllForAssessment(UUID assessmentId, UUID currentUserId) {
        AssessmentSecurePool pool = assessmentSecurePoolRepository.findByAssessmentId(assessmentId).orElse(null);
        if (pool == null) {
            return;
        }
        Instant now = Instant.now();
        for (Question question : questionRepository.findBySecurePoolId(pool.getId())) {
            question.setLocked(false);
            question.setConfidentiality(QuestionConfidentiality.RELEASED);
            question.setSecurePoolId(null);
            questionRepository.save(question);
        }
        assessmentSecurePoolRepository.deleteByAssessmentId(assessmentId);
    }

    @Transactional
    public void releaseIfAutoAfterClose(UUID assessmentId) {
        AssessmentSecurePool pool = assessmentSecurePoolRepository.findByAssessmentId(assessmentId).orElse(null);
        if (pool == null
                || pool.getReleaseMode() != AssessmentSecurePoolReleaseMode.AUTO_AFTER_CLOSE
                || pool.getStatus() != AssessmentSecurePoolStatus.SEALED) {
            return;
        }
        Instant now = Instant.now();
        pool.setStatus(AssessmentSecurePoolStatus.RELEASED);
        pool.setReleasedAt(now);
        AssessmentSecurePool savedPool = assessmentSecurePoolRepository.save(pool);

        for (Question question : questionRepository.findBySecurePoolId(savedPool.getId())) {
            question.setConfidentiality(QuestionConfidentiality.RELEASED);
            questionRepository.save(question);
        }
    }
}
