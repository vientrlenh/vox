package com.sep.vox.application.port.input.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.personalization.PracticeSessionRepository;

@Service
public class PracticeSessionCleanupService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(PracticeSessionCleanupService.class);

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeGradingFlushService gradingFlushService;
    private final PracticeSessionClosedHandler sessionClosedHandler;

    public PracticeSessionCleanupService(
            PracticeSessionRepository practiceSessionRepository,
            PracticeGradingFlushService gradingFlushService,
            PracticeSessionClosedHandler sessionClosedHandler) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.gradingFlushService = gradingFlushService;
        this.sessionClosedHandler = sessionClosedHandler;
    }

    @Transactional
    public int cleanupStaleSessions(Instant staleBefore) {
        var stale = practiceSessionRepository.findStaleInProgress(staleBefore);
        for (var session : stale) {
            // Phiên rớt mạng là diện DỄ có câu dở nhất -- học sinh không tự bấm kết thúc thì
            // gần như chắc chắn đang nói dở một câu. Không xả ở đây thì đúng nhóm cần nhất
            // lại là nhóm mất trắng.
            gradingFlushService.flush(session.getId());
            // Cùng phép đo với EndPracticeSessionUseCase: CÓ NÓI hay không, chứ không phải
            // đã chấm xong hay chưa -- xem chú thích dài ở đó để biết vì sao.
            //
            // Riêng ở đây có sắc thái thêm: phiên tới được job dọn nghĩa là học sinh KHÔNG
            // tự bấm kết thúc (rớt mạng, đóng app). Nhưng nếu em ấy đã nói thật thì công sức
            // đó vẫn phải được tính -- lượt nói đã ghi, đã chấm, đã vào hồ sơ điểm yếu.
            var spoke = session.getGradedSeconds() > 0;
            var status = spoke ? "COMPLETED" : "ABANDONED";
            // UNKNOWN thẳng, KHÔNG gọi SessionDiagnosisPolicy.
            //
            // Phiên tới được job dọn nghĩa là client không kịp gửi gì -- ta không có
            // helpRequestCount lẫn longPauseCount. Bản trước truyền 0/0 vào chính sách chẩn
            // đoán, mà 0/0 làm hai vế hành vi của luật luôn đúng/luôn sai, nên chính sách
            // thoái hoá thành "điểm >= 0,65 thì kết luận CHÁN". Học sinh đóng app mà chưa nói
            // câu nào, điểm buổi TRƯỚC lại cao -> bị ghi là chán chủ đề này.
            //
            // Hậu quả không dừng ở một cái nhãn sai: recordSessionOutcome chỉ ghi sự kiện sở
            // thích cho phiên dở khi diagnosis == BORED, nên mỗi lần đoán nhầm là một lần hạ
            // điểm quan tâm của chủ đề dựa trên bằng chứng không tồn tại.
            //
            // Không biết thì nói không biết. UNKNOWN không sinh sự kiện nào cả.
            var diagnosis = spoke ? null : "UNKNOWN";
            practiceSessionRepository.save(session.closedAsStale(
                status,
                diagnosis,
                session.getLastHeartbeatAt()
            ));
            // ĐÚNG phần việc mà EndPracticeSessionUseCase làm sau khi đóng phiên. Thiếu ở đây
            // là thiếu với ĐA SỐ phiên, không phải trường hợp hiếm: đo trên dữ liệu thật, 4
            // trong 5 phiên gần nhất đi qua chính đường này -- học sinh chỉ đóng app chứ không
            // bấm "Hoàn tất". Với 4 phiên đó thì điểm quan tâm không cập nhật và không sinh
            // chủ đề mới, nên luyện bao nhiêu buổi danh sách chủ đề vẫn đứng yên.
            try {
                sessionClosedHandler.afterClosed(
                    session.getStudentId(), session.getId(), status, diagnosis
                );
            } catch (RuntimeException exception) {
                // Bắt ở ĐÂY, ngoài ranh giới transaction REQUIRES_NEW của afterClosed. Bắt bên
                // trong thì vô tác dụng: Spring vẫn ném UnexpectedRollbackException lúc commit
                // vì transaction đã bị đánh dấu rollback-only. Đo được thật -- cả job chết,
                // không phiên nào được đóng, chúng nằm IN_PROGRESS qua nhiều lượt chạy.
                LOGGER.warn("Xử lý sau khi đóng phiên {} thất bại.", session.getId(), exception);
            }
        }
        return stale.size();
    }
}
