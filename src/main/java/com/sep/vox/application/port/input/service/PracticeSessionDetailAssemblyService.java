package com.sep.vox.application.port.input.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.practicesession.PracticeSessionResponseMapper;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.TeacherPracticeSessionDetail;
import com.sep.vox.domain.dto.personalization.TeacherPracticeSessionDetailDto;
import com.sep.vox.domain.dto.personalization.TeacherPracticeTurnViewDto;
import com.sep.vox.domain.repository.PracticeCriterionScoreRepository;
import com.sep.vox.domain.repository.PracticeItemEvaluationRepository;
import com.sep.vox.domain.repository.PracticeItemResponseRepository;
import com.sep.vox.domain.repository.PracticeResponseTurnRepository;
import com.sep.vox.domain.repository.TurnCorrectionRepository;

/**
 * Dựng bản chi tiết một phiên luyện (lượt nói + sửa lỗi + điểm theo tiêu chí), KHÔNG kiểm quyền.
 *
 * Tách riêng vì có HAI đường vào cùng nội dung này với hai luật quyền khác hẳn nhau: giáo viên
 * xem bài của học sinh mình dạy ({@code studentPracticeSessionDetail}), và học sinh xem lại bài
 * của chính mình sau khi kết thúc phiên ({@code myPracticeSessionDetail}). Nếu để mỗi use case
 * tự dựng lấy thì hai bản sao sẽ trôi lệch nhau -- thêm một cột vào màn tổng kết của học sinh
 * mà quên bên giáo viên, hoặc ngược lại.
 *
 * Việc kiểm quyền cố ý nằm NGOÀI service này, ở từng use case: nó không biết ai đang gọi, nên
 * gọi thẳng vào đây mà quên kiểm quyền là lộ bài của người khác. Đọc kỹ điểm này trước khi thêm
 * đường vào thứ ba.
 */
@Service
public class PracticeSessionDetailAssemblyService {

    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final PracticeResponseTurnRepository practiceResponseTurnRepository;
    private final TurnCorrectionRepository turnCorrectionRepository;
    private final PracticeCriterionScoreRepository practiceCriterionScoreRepository;
    private final PracticeItemEvaluationRepository practiceItemEvaluationRepository;
    private final PracticeItemResponseRepository practiceItemResponseRepository;

    public PracticeSessionDetailAssemblyService(
            PracticeSessionQueryRepository practiceSessionQueryRepository,
            PracticeResponseTurnRepository practiceResponseTurnRepository,
            TurnCorrectionRepository turnCorrectionRepository,
            PracticeCriterionScoreRepository practiceCriterionScoreRepository,
            PracticeItemEvaluationRepository practiceItemEvaluationRepository,
            PracticeItemResponseRepository practiceItemResponseRepository) {
        this.practiceSessionQueryRepository = practiceSessionQueryRepository;
        this.practiceResponseTurnRepository = practiceResponseTurnRepository;
        this.turnCorrectionRepository = turnCorrectionRepository;
        this.practiceCriterionScoreRepository = practiceCriterionScoreRepository;
        this.practiceItemEvaluationRepository = practiceItemEvaluationRepository;
        this.practiceItemResponseRepository = practiceItemResponseRepository;
    }

    @Transactional(readOnly = true)
    public TeacherPracticeSessionDetail assemble(UUID sessionId) {
        var summary = practiceSessionQueryRepository.findSessionRowById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên luyện."));
        var turns = practiceResponseTurnRepository.findBySessionIdOrderByTurnOrder(sessionId).stream()
            .map(turn -> new TeacherPracticeTurnViewDto(
                turn.turnOrder(),
                turn.transcript(),
                turn.audioUrl(),
                turn.wordFeedbackJson(),
                turn.turnScore(),
                turnCorrectionRepository.findByTurnIdOrderById(turn.id())
            ))
            .toList();
        var scores = practiceCriterionScoreRepository.findScoresBySessionId(sessionId);
        var itemCount = practiceItemEvaluationRepository.countCompletedBySessionId(sessionId);
        var duration = summary.getEndedAt() == null
            ? 0
            : (int) Duration.between(summary.getStartedAt(), summary.getEndedAt()).toSeconds();
        return PracticeSessionResponseMapper.toResponse(new TeacherPracticeSessionDetailDto(
            sessionId,
            summary.getTopicName(),
            summary.getStartedAt().toString(),
            duration,
            itemCount,
            summary.getOverallScore(),
            scores,
            "COMPLETED".equals(summary.getStatus()),
            practiceItemResponseRepository.countAwaitingEvaluation(sessionId),
            // Số câu ĐÃ BỎ CUỘC. Tách khỏi "đang chờ" vì hai chuyện khác hẳn nhau: chờ thì rồi
            // sẽ có, bỏ cuộc thì không bao giờ. Gộp làm một là bắt học sinh ngồi quay vòng chờ
            // một kết quả sẽ không tới.
            practiceItemResponseRepository.countGradingGaveUp(
                sessionId, PracticeGradingFlushService.MAX_GRADING_ATTEMPTS
            ),
            practiceItemResponseRepository.findAverageDifficultyRank(sessionId),
            turns,
            summary.getScoreScaleMin(),
            summary.getScoreScaleMax()
        ));
    }
}
