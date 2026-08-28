package com.sep.vox.application.port.input.usecase.exam;

import java.time.Instant;
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

    /**
     * TẮT CÓ CHỦ ĐÍCH -- gắn câu hỏi vào kỳ thi không còn niêm phong câu hỏi đó nữa.
     *
     * <p>Trước đây hàm này dựng một {@code ExamSecurePool} rồi đặt {@code locked=true},
     * {@code confidentiality=EXAM_RESTRICTED} và trỏ {@code securePoolId} vào pool. Bỏ vì mức bảo
     * mật ấy KHÔNG bảo vệ gì thật: cả {@code findAccessible} lẫn {@code findAccessibleForExamPaper}
     * đều không lọc theo {@code confidentiality}, nên câu hỏi vẫn hiện ra với mọi người và vẫn chọn
     * được cho kỳ thi khác. Nó chỉ dán một cái nhãn nói sai về trạng thái thật.
     *
     * <p>Cũng thôi đặt {@code locked}: bỏ pool thì không còn đường nào tìm lại câu hỏi để mở khoá,
     * nên đặt khoá là khoá vĩnh viễn.
     *
     * <p>Không mất gì khi bỏ cờ đó, vì một câu hỏi muốn vào được kỳ thi hay khung đề thì BẮT BUỘC
     * phải {@code PUBLISHED} ({@code findAccessibleForExamPaper} lọc {@code q.status = 'PUBLISHED'}),
     * mà {@code PUBLISHED} tự nó đã nằm trong phép kiểm {@code immutable} ở mọi đường sửa:
     * {@code status == PUBLISHED || isLocked() || existsUsedInExam(...)}. Nghĩa là vế đầu đã luôn
     * đúng trước cả khi hai vế sau được xét -- câu hỏi trong kỳ thi vẫn phải nhân bản mới sửa được.
     *
     * <p>Giữ nguyên chữ ký và các nơi gọi để bật lại chỉ bằng cách hoàn nguyên thân hàm này.
     */
    @Transactional
    public void lockQuestionForExam(
            UUID questionId,
            UUID examId,
            ExamSecurePoolReleaseMode releaseModeIfCreating,
            UUID currentUserId) {
        // Không làm gì -- xem javadoc. Giữ nguyên thân hàm cũ bên dưới để bật lại chỉ bằng cách
        // bỏ chú thích, không phải viết lại từ đầu.
        //
        // var now = Instant.now();
        // var pool = examSecurePoolRepository.findByExamId(examId)
        //     .orElseGet(() -> examSecurePoolRepository.save(new ExamSecurePool(
        //         examId,
        //         ExamSecurePoolStatus.SEALED,
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
        //     .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        // question.setLocked(true);
        // question.setConfidentiality(QuestionConfidentiality.EXAM_RESTRICTED);
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
    public void releaseAllForExam(UUID examId, UUID currentUserId) {
        var pool = examSecurePoolRepository.findByExamId(examId).orElse(null);
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
        examSecurePoolRepository.deleteByExamId(examId);
    }

    @Transactional
    public void releaseIfAutoAfterClose(UUID examId) {
        var pool = examSecurePoolRepository.findByExamId(examId).orElse(null);
        if (pool == null
                || pool.getReleaseMode() != ExamSecurePoolReleaseMode.AUTO_AFTER_CLOSE
                || pool.getStatus() != ExamSecurePoolStatus.SEALED) {
            return;
        }
        var now = Instant.now();
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
