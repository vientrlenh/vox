package com.sep.vox.application.port.input.usecase.assessment;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolStatus;
import com.sep.vox.domain.model.question.QuestionConfidentiality;
import com.sep.vox.domain.repository.assessment.AssessmentSecurePoolRepository;
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
     * TẮT CÓ CHỦ ĐÍCH -- gắn câu hỏi vào kỳ thi không còn niêm phong câu hỏi đó nữa.
     *
     * <p>Trước đây hàm này dựng một {@code AssessmentSecurePool} rồi đặt {@code locked=true},
     * {@code confidentiality=ASSESSMENT_RESTRICTED} và trỏ {@code securePoolId} vào pool. Bỏ vì mức bảo
     * mật ấy KHÔNG bảo vệ gì thật: cả {@code findAccessible} lẫn {@code findAccessibleForAssessmentPaper}
     * đều không lọc theo {@code confidentiality}, nên câu hỏi vẫn hiện ra với mọi người và vẫn chọn
     * được cho kỳ thi khác. Nó chỉ dán một cái nhãn nói sai về trạng thái thật.
     *
     * <p>Cũng thôi đặt {@code locked}: bỏ pool thì không còn đường nào tìm lại câu hỏi để mở khoá,
     * nên đặt khoá là khoá vĩnh viễn.
     *
     * <p>Không mất gì khi bỏ cờ đó, vì một câu hỏi muốn vào được kỳ thi hay khung đề thì BẮT BUỘC
     * phải {@code PUBLISHED} ({@code findAccessibleForAssessmentPaper} lọc {@code q.status = 'PUBLISHED'}),
     * mà {@code PUBLISHED} tự nó đã nằm trong phép kiểm {@code immutable} ở mọi đường sửa:
     * {@code status == PUBLISHED || isLocked() || existsUsedInAssessment(...)}. Nghĩa là vế đầu đã luôn
     * đúng trước cả khi hai vế sau được xét -- câu hỏi trong kỳ thi vẫn phải nhân bản mới sửa được.
     *
     * <p>Giữ nguyên chữ ký và các nơi gọi để bật lại chỉ bằng cách hoàn nguyên thân hàm này.
     */
    @Transactional
    public void lockQuestionForAssessment(
            UUID questionId,
            UUID assessmentId,
            AssessmentSecurePoolReleaseMode releaseModeIfCreating,
            UUID currentUserId) {
        // Không làm gì -- xem javadoc. Giữ nguyên thân hàm cũ bên dưới để bật lại chỉ bằng cách
        // bỏ chú thích, không phải viết lại từ đầu.
        //
        // var now = Instant.now();
        // var pool = assessmentSecurePoolRepository.findByAssessmentId(assessmentId)
        //     .orElseGet(() -> assessmentSecurePoolRepository.save(new AssessmentSecurePool(
        //         assessmentId,
        //         AssessmentSecurePoolStatus.SEALED,
        //         releaseModeIfCreating,
        //         null,
        //         null,
        //         null,
        //         now,
        //         now,
        //         currentUserId,
        //         currentUserId
        //     )));
        //
        // var question = questionRepository.findById(questionId)
        //     .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        // question.setLocked(true);
        // question.setConfidentiality(QuestionConfidentiality.ASSESSMENT_RESTRICTED);
        // question.setSecurePoolId(pool.getId());
        // question.setUpdatedAt(now);
        // question.setUpdatedBy(currentUserId);
        // questionRepository.save(question);
    }

    @Transactional
    public void unlockQuestion(UUID questionId, UUID currentUserId) {
        var question = questionRepository.findById(questionId).orElse(null);
        if (question == null || !question.isLocked()) {
            return;
        }
        question.setLocked(false);
        question.setConfidentiality(QuestionConfidentiality.RELEASED);
        question.setSecurePoolId(null);
        question.setUpdatedAt(Instant.now());
        question.setUpdatedBy(currentUserId);
        questionRepository.save(question);
    }

    /**
     * Xoá hẳn kỳ thi thì kho câu hỏi mật của nó cũng biến mất, nên phải mở khoá mọi câu hỏi đang
     * trỏ vào pool trước rồi mới xoá pool. Bỏ bước này là câu hỏi nằm lại ngân hàng đề ở trạng thái
     * khoá với securePoolId trỏ vào một pool không còn tồn tại — không còn đường nào giải phóng.
     */
    @Transactional
    public void releaseAllForAssessment(UUID assessmentId, UUID currentUserId) {
        var pool = assessmentSecurePoolRepository.findByAssessmentId(assessmentId).orElse(null);
        if (pool == null) {
            return;
        }
        var now = Instant.now();
        for (var question : questionRepository.findBySecurePoolId(pool.getId())) {
            question.setLocked(false);
            question.setConfidentiality(QuestionConfidentiality.RELEASED);
            question.setSecurePoolId(null);
            question.setUpdatedAt(now);
            question.setUpdatedBy(currentUserId);
            questionRepository.save(question);
        }
        assessmentSecurePoolRepository.deleteByAssessmentId(assessmentId);
    }

    @Transactional
    public void releaseIfAutoAfterClose(UUID assessmentId) {
        var pool = assessmentSecurePoolRepository.findByAssessmentId(assessmentId).orElse(null);
        if (pool == null
                || pool.getReleaseMode() != AssessmentSecurePoolReleaseMode.AUTO_AFTER_CLOSE
                || pool.getStatus() != AssessmentSecurePoolStatus.SEALED) {
            return;
        }
        var now = Instant.now();
        pool.setStatus(AssessmentSecurePoolStatus.RELEASED);
        pool.setReleasedAt(now);
        pool.setUpdatedAt(now);
        var savedPool = assessmentSecurePoolRepository.save(pool);

        for (var question : questionRepository.findBySecurePoolId(savedPool.getId())) {
            question.setConfidentiality(QuestionConfidentiality.RELEASED);
            question.setUpdatedAt(now);
            questionRepository.save(question);
        }
    }
}
