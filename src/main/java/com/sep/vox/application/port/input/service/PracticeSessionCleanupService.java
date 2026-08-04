package com.sep.vox.application.port.input.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.personalization.PracticeItemEvaluationRepository;
import com.sep.vox.domain.repository.personalization.PracticeSessionRepository;
import com.sep.vox.domain.service.personalization.SessionDiagnosisPolicy;

@Service
public class PracticeSessionCleanupService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeItemEvaluationRepository practiceItemEvaluationRepository;
    private final PracticeGradingFlushService gradingFlushService;

    public PracticeSessionCleanupService(
            PracticeSessionRepository practiceSessionRepository,
            PracticeItemEvaluationRepository practiceItemEvaluationRepository,
            PracticeGradingFlushService gradingFlushService) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.practiceItemEvaluationRepository = practiceItemEvaluationRepository;
        this.gradingFlushService = gradingFlushService;
    }

    @Transactional
    public int cleanupStaleSessions(Instant staleBefore) {
        var stale = practiceSessionRepository.findStaleInProgress(staleBefore);
        for (var session : stale) {
            // Phiên rớt mạng là diện DỄ có câu dở nhất -- học sinh không tự bấm kết thúc thì
            // gần như chắc chắn đang nói dở một câu. Không xả ở đây thì đúng nhóm cần nhất
            // lại là nhóm mất trắng.
            gradingFlushService.flush(session.getId());
            var score = practiceItemEvaluationRepository.findLastValidNormalizedScore(session.getId());
            // Cùng phép đo với EndPracticeSessionUseCase: CÓ NÓI hay không, chứ không phải
            // đã chấm xong hay chưa -- xem chú thích dài ở đó để biết vì sao.
            //
            // Riêng ở đây có sắc thái thêm: phiên tới được job dọn nghĩa là học sinh KHÔNG
            // tự bấm kết thúc (rớt mạng, đóng app). Nhưng nếu em ấy đã nói thật thì công sức
            // đó vẫn phải được tính -- lượt nói đã ghi, đã chấm, đã vào hồ sơ điểm yếu.
            var spoke = session.getGradedSeconds() > 0;
            var diagnosis = spoke ? null : SessionDiagnosisPolicy.diagnose(score, 0, 0);
            practiceSessionRepository.save(session.closedAsStale(
                spoke ? "COMPLETED" : "ABANDONED",
                diagnosis,
                session.getLastHeartbeatAt()
            ));
        }
        return stale.size();
    }
}
